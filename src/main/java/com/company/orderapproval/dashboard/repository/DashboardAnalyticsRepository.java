package com.company.orderapproval.dashboard.repository;

import com.company.orderapproval.dashboard.dto.DashboardResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;
import java.util.UUID;

@Repository
public class DashboardAnalyticsRepository {
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DashboardAnalyticsRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public DashboardResponse aggregate(String dashboardType,
                                       UUID organizationId,
                                       UUID branchId,
                                       UUID customerId,
                                       int days) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(days - 1L);
        boolean includeOrganizations = "SUPER_ADMIN".equals(dashboardType);
        boolean includeBranches = includeOrganizations || "ORGANIZATION_ADMIN".equals(dashboardType);
        boolean includeCustomerAnalytics = includeBranches || "BRANCH_ADMIN".equals(dashboardType);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("filterOrganization", organizationId != null, Types.BOOLEAN)
                .addValue("filterBranch", branchId != null, Types.BOOLEAN)
                .addValue("filterCustomer", customerId != null, Types.BOOLEAN)
                .addValue("includeOrganizations", includeOrganizations, Types.BOOLEAN)
                .addValue("includeBranches", includeBranches, Types.BOOLEAN)
                .addValue("includeCustomerAnalytics", includeCustomerAnalytics, Types.BOOLEAN)
                .addValue("organizationId", organizationId == null ? EMPTY_UUID : organizationId, Types.OTHER)
                .addValue("branchId", branchId == null ? EMPTY_UUID : branchId, Types.OTHER)
                .addValue("customerId", customerId == null ? EMPTY_UUID : customerId, Types.OTHER)
                .addValue("from", Timestamp.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()), Types.TIMESTAMP)
                .addValue("to", Timestamp.from(toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), Types.TIMESTAMP);

        Map<String, Object> row = jdbc.queryForMap(SQL, parameters);
        JsonNode overview = json(row.get("overview"));
        if (!"SUPER_ADMIN".equals(dashboardType) && overview instanceof ObjectNode object) {
            object.remove("organizationCount");
        }
        JsonNode organizationBreakdown = json(row.get("organization_breakdown"));
        JsonNode branchBreakdown = json(row.get("branch_breakdown"));
        JsonNode topCustomers = json(row.get("top_customers"));

        if ("ORGANIZATION_ADMIN".equals(dashboardType)) {
            organizationBreakdown = null;
        } else if ("BRANCH_ADMIN".equals(dashboardType)) {
            organizationBreakdown = null;
            branchBreakdown = null;
            if (overview instanceof ObjectNode object) {
                object.remove("activeBranchCount");
            }
        } else if ("CUSTOMER_ADMIN".equals(dashboardType)
                || "CUSTOMER_USER".equals(dashboardType)) {
            organizationBreakdown = null;
            branchBreakdown = null;
            topCustomers = null;
            if (overview instanceof ObjectNode object) {
                object.remove("activeBranchCount");
                object.remove("businessCustomerCount");
                object.remove("activeDealingCustomers");
            }
        }
        return new DashboardResponse(
                dashboardType,
                new DashboardResponse.DashboardScope(organizationId, branchId, customerId),
                fromDate,
                toDate,
                overview,
                json(row.get("status_counts")),
                json(row.get("approval_load")),
                organizationBreakdown,
                branchBreakdown,
                topCustomers,
                json(row.get("top_locations")),
                json(row.get("top_orders"))
        );
    }

    private JsonNode json(Object value) {
        try {
            return objectMapper.readTree(String.valueOf(value));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to read dashboard aggregate", ex);
        }
    }

    private static final String SQL = """
        WITH scoped_orders AS (
            SELECT o.*
            FROM orders o
            WHERE (:filterOrganization = FALSE OR o.organization_id = :organizationId)
              AND (:filterBranch = FALSE OR o.branch_id = :branchId)
              AND (:filterCustomer = FALSE OR o.business_customer_id = :customerId)
              AND o.created_at >= :from AND o.created_at < :to
        ),
        order_totals AS (
            SELECT so.id, COALESCE(SUM(oi.line_total), 0) total_amount
            FROM scoped_orders so LEFT JOIN order_items oi ON oi.order_id = so.id
            GROUP BY so.id
        ),
        status_data AS (
            SELECT status, COUNT(*) count FROM scoped_orders GROUP BY status
        ),
        customer_data AS (
            SELECT so.business_customer_id id, MAX(so.business_customer_name) name,
                   COUNT(*) order_count, COALESCE(SUM(ot.total_amount), 0) order_value
            FROM scoped_orders so JOIN order_totals ot ON ot.id = so.id
            GROUP BY so.business_customer_id ORDER BY order_count DESC, order_value DESC LIMIT 10
        ),
        location_data AS (
            SELECT COALESCE(NULLIF(bcl.location_name, ''), NULLIF(so.location, ''), 'Unknown') name,
                   COUNT(*) order_count
            FROM scoped_orders so
            LEFT JOIN business_customer_locations bcl ON bcl.id = so.business_customer_location_id
            GROUP BY 1 ORDER BY order_count DESC LIMIT 10
        ),
        top_order_data AS (
            SELECT so.id, so.order_number, so.business_customer_name, so.status,
                   so.created_at, ot.total_amount
            FROM scoped_orders so JOIN order_totals ot ON ot.id = so.id
            ORDER BY ot.total_amount DESC, so.created_at DESC LIMIT 10
        ),
        branch_data AS (
            SELECT b.id, b.name, b.branch_code,
                   COUNT(DISTINCT bc.id) business_customer_count,
                   COUNT(DISTINCT so.id) order_count
            FROM branches b
            LEFT JOIN business_customers bc ON bc.branch_id = b.id AND bc.status = 'ACTIVE'
            LEFT JOIN scoped_orders so ON so.branch_id = b.id
            WHERE (:filterOrganization = FALSE OR b.organization_id = :organizationId)
              AND (:filterBranch = FALSE OR b.id = :branchId)
              AND b.status = 'ACTIVE'
            GROUP BY b.id, b.name, b.branch_code ORDER BY order_count DESC, b.name LIMIT 20
        ),
        organization_data AS (
            SELECT org.id, org.name, org.organization_code,
                   COUNT(DISTINCT b.id) active_branch_count,
                   COUNT(DISTINCT bc.id) business_customer_count,
                   COUNT(DISTINCT so.id) order_count
            FROM organizations org
            LEFT JOIN branches b ON b.organization_id = org.id AND b.status = 'ACTIVE'
            LEFT JOIN business_customers bc ON bc.organization_id = org.id AND bc.status = 'ACTIVE'
            LEFT JOIN scoped_orders so ON so.organization_id = org.id
            WHERE (:filterOrganization = FALSE OR org.id = :organizationId)
              AND org.status = 'ACTIVE'
            GROUP BY org.id, org.name, org.organization_code ORDER BY order_count DESC, org.name LIMIT 20
        )
        SELECT
          (jsonb_build_object(
            'totalOrders', (SELECT COUNT(*) FROM scoped_orders),
            'totalOrderValue', (SELECT COALESCE(SUM(total_amount),0) FROM order_totals)
          )
          || CASE WHEN :includeOrganizations THEN jsonb_build_object(
               'organizationCount', (SELECT COUNT(*) FROM organizations org WHERE org.status='ACTIVE')
             ) ELSE '{}'::jsonb END
          || CASE WHEN :includeBranches THEN jsonb_build_object(
               'activeBranchCount', (SELECT COUNT(*) FROM branches b WHERE b.status='ACTIVE' AND (:filterOrganization = FALSE OR b.organization_id=:organizationId))
             ) ELSE '{}'::jsonb END
          || CASE WHEN :includeCustomerAnalytics THEN jsonb_build_object(
               'businessCustomerCount', (SELECT COUNT(*) FROM business_customers bc WHERE bc.status='ACTIVE' AND (:filterOrganization = FALSE OR bc.organization_id=:organizationId) AND (:filterBranch = FALSE OR bc.branch_id=:branchId)),
               'activeDealingCustomers', (SELECT COUNT(DISTINCT business_customer_id) FROM scoped_orders)
             ) ELSE '{}'::jsonb END) overview,
          COALESCE((SELECT jsonb_object_agg(status, count) FROM status_data), '{}'::jsonb) status_counts,
          jsonb_build_object(
            'awaitingApproval', (SELECT COUNT(*) FROM scoped_orders WHERE status='CREATED'),
            'partiallyApproved', (SELECT COUNT(*) FROM scoped_orders so WHERE so.status='CREATED' AND EXISTS (SELECT 1 FROM order_approvers oa WHERE oa.order_id=so.id AND oa.approval_status='APPROVED') AND EXISTS (SELECT 1 FROM order_approvers oa WHERE oa.order_id=so.id AND oa.approval_status='PENDING')),
            'pendingApproverActions', (SELECT COUNT(*) FROM order_approvers oa JOIN scoped_orders so ON so.id=oa.order_id WHERE oa.approval_status='PENDING')
          ) approval_load,
          CASE WHEN :includeOrganizations THEN COALESCE((SELECT jsonb_agg(to_jsonb(x)) FROM organization_data x), '[]'::jsonb) ELSE NULL::jsonb END organization_breakdown,
          CASE WHEN :includeBranches THEN COALESCE((SELECT jsonb_agg(to_jsonb(x)) FROM branch_data x), '[]'::jsonb) ELSE NULL::jsonb END branch_breakdown,
          CASE WHEN :includeCustomerAnalytics THEN COALESCE((SELECT jsonb_agg(to_jsonb(x)) FROM customer_data x), '[]'::jsonb) ELSE NULL::jsonb END top_customers,
          COALESCE((SELECT jsonb_agg(to_jsonb(x)) FROM location_data x), '[]'::jsonb) top_locations,
          COALESCE((SELECT jsonb_agg(to_jsonb(x)) FROM top_order_data x), '[]'::jsonb) top_orders
        """;
}

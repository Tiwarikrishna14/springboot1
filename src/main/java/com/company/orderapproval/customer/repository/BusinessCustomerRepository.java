package com.company.orderapproval.customer.repository;
import com.company.orderapproval.customer.entity.BusinessCustomer; import com.company.orderapproval.customer.entity.BusinessCustomerStatus; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Modifying; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param; import java.util.*;
public interface BusinessCustomerRepository extends JpaRepository<BusinessCustomer,UUID>{

 Optional<BusinessCustomer> findByBranchIdAndCustomerCode(UUID branchId,String code);
 Optional<BusinessCustomerStatus> findStatusByBranchIdAndCustomerCode(UUID branchId,String code);
 Optional<BusinessCustomer> findByCustomerCodeIgnoreCase(String customerCode);
 boolean existsByBranchIdAndCustomerCodeAndIdNot(UUID branchId,String code,UUID id);
 long countByBranchId(UUID branchId);
 long countByOrganizationId(UUID organizationId);
 @Query("select c.customerCode from BusinessCustomer c where c.branchId=:branchId")
 List<String> findCustomerCodesByBranchId(@Param("branchId") UUID branchId);
 @Query("select c.customerCode from BusinessCustomer c where c.organizationId=:organizationId")
 List<String> findCustomerCodesByOrganizationId(@Param("organizationId") UUID organizationId);
 @Modifying
 @Query("update BusinessCustomer c set c.branchId=null, c.updatedBy=:updatedBy where c.branchId=:branchId")
 int detachBranchMapping(@Param("branchId") UUID branchId,@Param("updatedBy") UUID updatedBy);
 @Query("select c from BusinessCustomer c where (:organizationId is null or c.organizationId=:organizationId) and (:branchId is null or c.branchId=:branchId) and (:city is null or :city='' or lower(c.city)=lower(:city)) and (:status is null or c.status=:status) and (:search is null or :search='' or lower(c.name) like lower(concat('%',:search,'%')) or lower(c.customerCode) like lower(concat('%',:search,'%')))" )
 Page<BusinessCustomer> search(@Param("organizationId") UUID organizationId,@Param("branchId") UUID branchId,@Param("status") BusinessCustomerStatus status,@Param("city") String city,@Param("search") String search,Pageable p);

 @Query("select c from BusinessCustomer c where (:organizationId is null or c.organizationId=:organizationId) and upper(c.customerCode)=upper(:customerCode) and c.status=:status")
 List<BusinessCustomer> findByCustomerCodeInScope(@Param("organizationId") UUID organizationId,@Param("customerCode") String customerCode,@Param("status") BusinessCustomerStatus status);

 boolean existsByCustomerCode(String customerCode);

}


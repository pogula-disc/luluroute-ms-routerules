package com.luluroute.ms.routerules.business.carrier.repository;

import com.luluroute.ms.routerules.business.carrier.entity.EntityHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EntityHolidayRepository extends JpaRepository<EntityHoliday, UUID> {

    @Query(value = "SELECT * FROM DOMAIN.ENTTHOLIDAY H " +
            "            WHERE CAST(H.HOLIDAYID AS VARCHAR)   IN ( :holidayIds )" +
            "            AND H.HOLIDAYDATE >= :initialDate  AND H.HOLIDAYDATE <= :lastDate ", nativeQuery = true)
    public List<EntityHoliday> findHolidays(@Param("holidayIds") List<String> holidayIds,
                                            @Param("initialDate") LocalDate initialDate,
                                            @Param("lastDate") LocalDate lastDate);
}

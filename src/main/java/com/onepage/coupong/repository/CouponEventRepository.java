package com.onepage.coupong.repository;

import com.onepage.coupong.entity.CouponEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {
    List<CouponEvent> findAllByDate(LocalDateTime date);
}

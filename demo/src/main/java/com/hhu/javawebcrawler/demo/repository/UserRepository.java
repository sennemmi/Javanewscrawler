package com.hhu.javawebcrawler.demo.repository;

import com.hhu.javawebcrawler.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<实体类类型, 主键类型>
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 根据用户名查找用户。
     * Spring Data JPA会根据方法名自动生成查询。
     */
    Optional<User> findByUsername(String username);
}
package com.hhu.javawebcrawler.demo.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

// @Data是Lombok库的注解，它会自动为所有字段生成getter、setter、toString、equals和hashCode方法
@Data
// @Entity注解，声明这是一个JPA实体类，它将映射到数据库中的一个表
@Entity
// @Table注解，指定这个实体类映射到数据库中的表名为"t_user"
@Table(name = "t_user")
// 定义一个名为User的公共类
public class User {
    // @Id注解，标记这个字段是表的主键
    @Id
    // @GeneratedValue注解，指定主键的生成策略为自增长（通常由数据库控制）
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 声明一个私有的Long类型字段id，作为用户唯一标识
    private Long id;
    
    // @Column注解，配置字段映射的列属性，nullable=false表示该列不为空，unique=true表示该列值唯一
    @Column(nullable = false, unique = true)
    // 声明一个私有的String类型字段username，用于存储用户名
    private String username;

    // @Column注解，配置字段映射的列属性，nullable=false表示该列不为空
    @Column(nullable = false)
    // 声明一个私有的String类型字段password，用于存储用户密码
    private String password;
    
    // @Column注解，指定列名为"create_time"，并设置updatable=false表示此列的值在更新时不会被改变
    @Column(name = "create_time", updatable = false)
    // 声明一个私有的LocalDateTime类型字段createTime，用于记录创建时间
    private LocalDateTime createTime;
    
    // @Column注解，指定列名为"update_time"
    @Column(name = "update_time")
    // 声明一个私有的LocalDateTime类型字段updateTime，用于记录最后更新时间
    private LocalDateTime updateTime;
    
    // @PrePersist注解，这是一个JPA生命周期回调，该方法会在实体第一次被持久化（保存）到数据库之前调用
    @PrePersist
    // 定义一个受保护的onCreate方法，用于在创建实体时初始化时间戳
    protected void onCreate() {
        // 将当前时间赋值给createTime字段
        this.createTime = LocalDateTime.now();
        // 将当前时间赋值给updateTime字段
        this.updateTime = LocalDateTime.now();
    // onCreate方法结束
    }
    
    // @PreUpdate注解，这是一个JPA生命周期回调，该方法会在实体数据在数据库中被更新之前调用
    @PreUpdate
    // 定义一个受保护的onUpdate方法，用于在更新实体时更新时间戳
    protected void onUpdate() {
        // 将当前时间赋值给updateTime字段
        this.updateTime = LocalDateTime.now();
    // onUpdate方法结束
    }
// User类定义结束
}
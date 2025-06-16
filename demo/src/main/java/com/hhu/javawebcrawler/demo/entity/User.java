package com.hhu.javawebcrawler.demo.entity;
import lombok.Data; 
import jakarta.persistence.*; 
import java.time.LocalDateTime;

/**
 * 用户实体类，映射 t_user 表
 */
@Data// Lombok注解：自动为所有字段生成getter/setter、toString、equals、hashCode等方法
@Entity// 声明这是一个JPA实体类
@Table(name = "t_user")// 指定数据库表名
public class User {
    @Id// 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)// 自增策略
    private Long id;
    
    @Column(nullable = false, unique = true)// 数据不能为空且必须唯一
    private String username;

    @Column(nullable = false)
    private String password;
    
    @Column(name = "create_time",updatable = false)// 创建时间，不可更新
    private LocalDateTime createTime;
    
    @Column(name = "update_time")// 更新时间
    private LocalDateTime updateTime;
    
    /**
     * JPA生命周期回调方法，在实体被持久化之前自动调用
     * 用于设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * JPA生命周期回调方法，在实体被更新之前自动调用
     * 用于更新"update_time"字段
     */
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}

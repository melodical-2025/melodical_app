package com.melodical.backend.domain

import jakarta.persistence.*

@Entity
@Table(name = "post_ratings", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "post_id"])])
class PostRating(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: Post,

    @Column(nullable = false)
    var rating: Int
)
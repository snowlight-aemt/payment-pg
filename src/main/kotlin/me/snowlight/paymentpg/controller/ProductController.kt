package me.snowlight.paymentpg.controller

import me.snowlight.paymentpg.service.ProductService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductController(
    private val productService: ProductService,
) {
    @GetMapping("/product/{id}")
    suspend fun get(@PathVariable id: Long) = productService.get(id)

    @GetMapping("/product/all")
    suspend fun getAll() = productService.getAll()

}
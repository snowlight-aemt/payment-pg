package me.snowlight.paymentpg.controller

import me.snowlight.paymentpg.service.OrderService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable

@Controller
class ViewController(
    private val orderService: OrderService,
) {

    @GetMapping("/hello/{name}")
    suspend fun hello(@PathVariable name: String, model: Model): String {
        model.addAttribute("name", name)
        model.addAttribute("order", orderService.get(20))
        return "hello-world";
    }

    @GetMapping("/pay/{orderId}")
    suspend fun pay(@PathVariable orderId: Long, model: Model): String {
        model.addAttribute("order", orderService.get(orderId))
        return "pay"
    }

    @GetMapping("/pay/success")
    suspend fun paySuccess(request: ReqPaySucceed): String {
        if (!orderService.authSuccess(request))
            return "pay-fail"

        orderService.capture(request)
        return "pay-success"
    }

    @GetMapping("/pay/fail")
    suspend fun payFail(request: ReqPayFailed): String {
        orderService.authFailed(request);
        return "pay-fail"
    }
}
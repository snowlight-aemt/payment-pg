package me.snowlight.paymentpg.controller

import me.snowlight.paymentpg.service.OrderService
import me.snowlight.paymentpg.service.PaymentService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable

@Controller
class ViewController(
    private val orderService: OrderService,
    private val paymentService: PaymentService,
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

    // LEARN authSuccess , capture 를 분리해서 service 추가한 이유?
    //  - Controller 에서 순서대로 authSuccess -> capture 호출 된다
    //  - Service 에 새로운 메서드를 만들어서 호출하면 어떻까?
    @GetMapping("/pay/success")
    suspend fun paySuccess(request: ReqPaySucceed): String {
        if (!paymentService.authSuccess(request))
            return "pay-fail"

        paymentService.capture(request)
        return "pay-success"
    }

    @GetMapping("/pay/fail")
    suspend fun payFail(request: ReqPayFailed): String {
        paymentService.authFailed(request);
        return "pay-fail"
    }
}
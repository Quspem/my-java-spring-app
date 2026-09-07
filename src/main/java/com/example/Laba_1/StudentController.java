package com.example.Laba_1;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentController{
    @GetMapping("/Student")
    public String StudentNumberAndPi(){
        double pi = 0;
        for (double i = 1; i < 100_000_000; i += 4) {
            pi += (4.0 / i) - (4.0 / (i + 2));
        }
        return "Сытько Вадим Александрович<br>Группа 377<br>" + pi;
    }
}
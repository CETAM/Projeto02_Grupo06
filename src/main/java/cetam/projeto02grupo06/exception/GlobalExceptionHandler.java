package cetam.projeto02grupo06.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Rede de segurança para qualquer exceção não tratada explicitamente em um
 * controller. Sem isso, o Spring Boot mostra a "Whitelabel Error Page" com
 * stack trace exposto ao usuário final (TC-16 da suíte de testes).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String tratarErroGenerico(Exception e, Model model) {
        model.addAttribute("mensagem", "Ocorreu um erro inesperado. Tente novamente em instantes.");
        return "error";
    }
}

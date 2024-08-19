package corp;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.time.ZoneId;

import java.util.Map;
import java.util.Arrays;
import java.util.Optional;

@WebFilter(value = "/time/*")
public class TimezoneValidateFilter extends HttpFilter {
    private TemplateEngine templateEngine;

    @Override
    public void init() throws ServletException {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    protected void doFilter(HttpServletRequest req,
                            HttpServletResponse res,
                            FilterChain chain) throws IOException, ServletException {
        Map<String, String[]> parameters = req.getParameterMap();

        if (!parameters.isEmpty() && !parameters.containsKey("timezone")) {
            sendErrorResponse(res, "Invalid parameter. Only 'timezone' is allowed.");
            return;
        }

        String timezone = req.getParameter("timezone");

        if (timezone == null) {
            timezone = getTimezoneFromCookies(req).orElse(null);
            chain.doFilter(req, res);
        } else if (isValidTimezone(normalizeTimezone(timezone))) {
            chain.doFilter(req, res);
        } else {
            sendErrorResponse(res, "Invalid timezone");
        }
    }

    private void sendErrorResponse(HttpServletResponse res, String message) throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        Context context = new Context();
        context.setVariable("message", message);

        templateEngine.process("error", context, res.getWriter());
    }

    private Optional<String> getTimezoneFromCookies(HttpServletRequest req) {
        if (req.getCookies() != null) {
            return Arrays.stream(req.getCookies())
                    .filter(cookie -> "lastTimezone".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst();
        }
        return Optional.empty();
    }

    private boolean isValidTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String normalizeTimezone(String timezone) {
        return timezone.replace(" ", "+");
    }
}

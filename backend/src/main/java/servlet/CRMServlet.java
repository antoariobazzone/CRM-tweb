package servlet;

import Data.Employee;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import db.BaseManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utility.AuthenticationResult;
import utility.LoginHelper;
import utility.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

@WebServlet(name = "CRMServlet", urlPatterns = {"/employees/*", "/benefits/*", "/contracts/*", "/departments/*", "/positions/*",
        "/projects/*", })
public class CRMServlet extends HttpServlet {
    LoginHelper loginHelper = new LoginHelper();
    private Gson gson;

    private static RequestBody getRequestBody(HttpServletRequest request) throws IOException {
        String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        BaseManager<?> manager = ManagerFactory.getManager(request.getServletPath());
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return new RequestBody(requestBody, manager, type);
    }

    public void init() {
        gson = new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        RequestBody body = getRequestBody(request);
        Map<String, Object> requestMap = gson.fromJson(body.requestBody(), body.type());
        Object entity = null;

        BaseManager<?> manager = ManagerFactory.getManager(request.getServletPath());
        String idParam = request.getParameter("id");
        String roleParam = request.getParameter("role");
        String preToken = request.getHeader("Authorization");

        AuthenticationResult result = getAuthenticationResult(preToken, out);
        if (result == null) return;

        if(roleParam != null) {
            try {
                int role = parseInt(roleParam);
                if(role == 0) entity = manager.loadAll();
                else if(role == 1) entity = manager.loadManagerView(result.getDepartment_id());
                if (entity != null) out.println(entity);
                return;
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }

        if (idParam != null) {
            try {
                int id = Integer.parseInt(idParam);
                entity = manager.loadById(id);
                if (entity != null) out.println(gson.toJson(entity));
                else response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else out.println(manager.loadAll());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String preToken = request.getHeader("Authorization");

        AuthenticationResult result = getAuthenticationResult(preToken, out);
        if (result == null) return;

        RequestBody body = getRequestBody(request);
        Map<String, Object> requestMap = gson.fromJson(body.requestBody(), body.type());

        switch (result.getRole()) {
            case 0:
                this.printResult(out, -1, "you don't have right role");
                return;
            case 1:
                if (requestMap.get("department_id") != null) {
                    int departmentIdFromRequest = ((Double) requestMap.get("department_id")).intValue();
                    if (departmentIdFromRequest != result.getDepartment_id()) {
                        this.printResult(out, -1, "you cannot add data on different department");
                        return;
                    }
                }
        }

        try {
            out.println(body.manager().addFromParams(requestMap));
        } catch (Exception ex) {
            this.printResult(out, -1, ex.getMessage());
            ex.printStackTrace();
        }
    }

    private AuthenticationResult getAuthenticationResult(String preToken, PrintWriter out) {
        AuthenticationResult result = LoginHelper.authenticate(preToken, loginHelper);
        if (!result.isSuccess()) {
            this.printResult(out, -1, result.getErrorMessage());
            return null;
        }
        return result;
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        BufferedReader body = request.getReader();

        BaseManager<?> manager = ManagerFactory.getManager(request.getServletPath());
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> requestMap = gson.fromJson(body, type);

        String resultId = manager.updateFromParams(requestMap);
        out.println(resultId);
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        int idParam = parseInt(request.getParameter("id"));
        String preToken = request.getHeader("Authorization");

        AuthenticationResult result = getAuthenticationResult(preToken, out);
        if (result == null) return;

        if (result.getRole() == 0) {
            this.printResult(out, -1, "you don't have right role");
            return;
        }

        RequestBody body = getRequestBody(request);

        try {
            body.manager.deleteEntity(idParam);
            out.println(new Gson().toJson(new Response(0, "Successfully deleted")));
        } catch (Exception ex) {
            this.printResult(out, -1, ex.getMessage());
            ex.printStackTrace();
        }

    }

    public void destroy() {
    }

    private Map<String, Object> extractRequestMap(HttpServletRequest request) throws IOException {
        if ("application/json".equalsIgnoreCase(request.getContentType())) {
            String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            return new Gson().fromJson(requestBody, type);
        } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(request.getContentType())) {
            return request.getParameterMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue()[0]
                    ));
        } else {
            throw new IOException("Unsupported Content-Type: " + request.getContentType());
        }
    }

    private void printResult(PrintWriter out, int code, String message) {
        out.println(new Gson().toJson(new Response(code, message)));
    }

    private record RequestBody(String requestBody, BaseManager<?> manager, Type type) {
    }
}

package com.ruankao.api.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.api.FieldErrorVO;
import com.ruankao.common.api.R;
import com.ruankao.common.exception.BizException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常转换（06 §6.3）。
 *
 * <p>Controller 与 Service <b>都不写 try-catch</b>，所有异常汇聚到这里转成 {@link R}。
 *
 * <p>HTTP 状态码：只有 401（未登录）与 403（无权限）例外，其余业务错误一律 200
 * （05 §5.1.1）。这样前端拦截器只需处理两个特殊状态，业务分支全部读 {@code code}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：按它携带的错误码返回，不改写文案。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        // 业务异常是预期内的控制流，记 DEBUG：线上默认 INFO 级别下不输出，
        // 需要排查时打开这个类的 DEBUG 即可，不会污染正常日志。
        if (log.isDebugEnabled()) {
            log.debug("业务异常 code={} message={} traceId={}",
                    errorCode.getCode(), ex.getMessage(), MDC.get(R.TRACE_ID_MDC_KEY));
        }
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(R.fail(errorCode, ex.getMessage(), null));
    }

    /** {@code @RequestBody} 上 Jakarta Validation 校验失败：返回 1001 + 字段级错误列表。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<List<FieldErrorVO>>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldErrorVO> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        log.debug("参数校验失败 {} 项 traceId={}", errors.size(), MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(R.fail(ErrorCode.PARAM_INVALID, errors));
    }

    /** 方法参数上的约束注解（如 {@code @PathVariable @Min(1)}）校验失败：同样返回 1001。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<List<FieldErrorVO>>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorVO> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        log.debug("参数校验失败 {} 项 traceId={}", errors.size(), MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(R.fail(ErrorCode.PARAM_INVALID, errors));
    }

    /**
     * 控制器方法参数校验失败（Spring Framework 6.1 起的主路径）：同样返回 1001。
     *
     * <p>它<b>不是</b> {@link ConstraintViolationException}：Spring 6.1 把"控制器上
     * {@code @RequestParam} / {@code @PathVariable} 的约束注解校验失败"统一改抛这个类型，
     * 且它是 {@code ResponseStatusException} 的子类。不单独接住的话，请求会落进下面的兜底分支，
     * 被报成 {@code 1000 系统内部错误} 并按 ERROR 记一条堆栈——把"用户把 pageSize 传成 0"
     * 说成"系统挂了"，既误导前端提示，也让真故障淹没在噪音里。
     *
     * <p>前提是控制器类上标了 {@code @Validated}（06 §6.3 的统一策略）：没有它，参数上的
     * 约束注解<b>静默不生效</b>，非法入参一路进 Service，这条分支也就永远不会被触发。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<R<List<FieldErrorVO>>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<FieldErrorVO> errors = ex.getAllValidationResults().stream()
                .map(this::toFieldError)
                .toList();
        log.debug("参数校验失败 {} 项 traceId={}", errors.size(), MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(R.fail(ErrorCode.PARAM_INVALID, errors));
    }

    /**
     * 参数类型转换失败（{@code ?pageSize=abc}、{@code ?onlyConfigured=1}）：返回 1001。
     *
     * <p>与上面两类同源：这是<b>入参不合法</b>，不是服务端故障。不单独接住的话会落进兜底分支
     * 报 {@code 1000} 并记 ERROR 堆栈——一个拼错的查询参数就能制造一条"系统内部错误"。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<List<FieldErrorVO>>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName() != null ? ex.getName() : "参数";
        List<FieldErrorVO> errors = List.of(new FieldErrorVO(name, "参数类型不正确"));
        log.debug("参数类型不匹配 name={} traceId={}", name, MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(R.fail(ErrorCode.PARAM_INVALID, errors));
    }

    /**
     * 请求体不是合法 JSON：返回 1001。
     *
     * <p>{@code @RequestBody} 反序列化失败。与上面几类同源——这是<b>客户端把报文写错了</b>，
     * 不是服务端故障。不单独接住就会落进兜底分支报 {@code 1000} 并记 ERROR 堆栈：
     * 一个手抖的 JSON 就能制造一条"系统内部错误"，把真故障淹掉。
     *
     * <p>字段名固定写 {@code body}：这类失败<b>没有</b>可归属的具体字段（连 JSON 都没解析出来），
     * 前端应把提示挂在请求体整体上，而不是某个输入框。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<List<FieldErrorVO>>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("请求体不可读 traceId={} message={}", MDC.get(R.TRACE_ID_MDC_KEY), ex.getMessage());
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(R.fail(ErrorCode.PARAM_INVALID, List.of(new FieldErrorVO("body", "请求体不是合法的 JSON"))));
    }

    /**
     * 缺少必填的查询参数：返回 1001。
     *
     * <p>{@code ?subjectId=} 整个没给时抛这个；<b>给了但类型不对</b>是
     * {@link MethodArgumentTypeMismatchException}，已由上面那条接住。两者同属"客户端漏了东西"，
     * 都不该报成系统内部错误。
     *
     * <p>字段名取 {@code ex.getParameterName()}——它正是客户端该补的那个参数名，前端能直接对上。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<List<FieldErrorVO>>> handleMissingParam(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName() != null ? ex.getParameterName() : "参数";
        log.debug("缺少必填参数 name={} traceId={}", name, MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(R.fail(ErrorCode.PARAM_INVALID, List.of(new FieldErrorVO(name, "缺少必填参数"))));
    }

    /**
     * {@code Content-Type} 不受支持：返回 1001。
     *
     * <p>例如用 {@code text/plain} 发一个本该是 JSON 的请求体。同属客户端报文问题。
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<R<List<FieldErrorVO>>> handleMediaTypeUnsupported(HttpMediaTypeNotSupportedException ex) {
        log.debug("Content-Type 不受支持 contentType={} traceId={}", ex.getContentType(), MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.getHttpStatus())
                .body(R.fail(ErrorCode.PARAM_INVALID, List.of(new FieldErrorVO("Content-Type", "不支持的请求内容类型"))));
    }

    /**
     * 请求方法不受支持（如用 GET 打一个只有 PUT/DELETE 的路径）：返回 1003。
     *
     * <p><b>为什么归到 1003 而不是新建一个码</b>：从客户端视角看，"这个 URL + 这个方法"
     * 就是不存在的东西，与下面的路径不存在同义；而且不告诉对方"这里其实有别的可用方法"，
     * 避免拿方法差异去探测接口面。它与 {@link #handleNotFound} 共用同一个语义出口。
     *
     * <p>触发路径值得记一下：{@code GET /admin/outline/nodes} 会先匹配上
     * {@code /admin/outline/{id}} 这条只有 PUT/DELETE 的映射（{@code {id}} 取到 {@code nodes}），
     * 于是抛本异常——<b>任何段数相同的错误路径都可能走到这里</b>，不是罕见分支。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.debug("请求方法不受支持 method={} traceId={}", ex.getMethod(), MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(R.fail(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /**
     * 客户端声明不接受任何可产出的表示（如 {@code Accept: application/xml}）：返回 406。
     *
     * <p><b>这是 05 §5.1.1「除 401/403 外一律 200」的一条例外，而且无法避免</b>：
     * 响应体是 JSON，而客户端的 {@code Accept} 明确拒绝它，统一响应体根本没有交付的可能。
     * 与其让 Spring 在序列化阶段抛出、落进兜底记一条 ERROR 堆栈（那是加本条之前的实际行为，
     * 已实测），不如显式给出 406 且不带响应体——状态码本来就是它该有的语义。
     *
     * <p>记 DEBUG：这是客户端自己选的 {@code Accept} 造成的，不是服务端故障。
     * 本项目的 SPA（axios 默认同时接受 {@code application/json}、{@code text/plain} 与通配符）
     * 不会触发它，触发方通常是扫描器或手工 curl。
     *
     * <p>（写这段注释时踩过一次：{@code @code} 里若出现星号加斜杠会提前终止注释块，
     * 后面的中文会被当成 Java 代码解析，报一串"非法的字符"。故此处不写通配符的字面量。）
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        log.debug("客户端不接受任何可产出的表示 traceId={}", MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    /**
     * 路径不存在（404）。
     *
     * <p>不加这一条的话，它会落到下面的兜底分支，被报成 {@code 1000 系统内部错误} 并以 ERROR
     * 级别记一条带堆栈的日志——用户把 URL 打错一个字母、或任何人扫描路径，都会制造一条
     * "系统内部错误"。这既误导前端提示，也让真正的故障淹没在噪音里。
     *
     * <p>HTTP 状态码仍为 200（05 §5.1.1 只允许 401/403 例外），业务码用
     * {@code 1003 资源不存在}，与"请求的东西不存在"这一语义一致。
     *
     * <p>覆盖两类：{@link NoResourceFoundException} 由静态资源处理器抛出（Boot 默认把
     * {@code /**} 映射给资源处理，未匹配的路径都会走到这里）；{@link NoHandlerFoundException}
     * 仅在开启 {@code spring.mvc.throw-exception-if-no-handler-found} 时出现，一并兜住以免
     * 将来有人打开那个开关后行为突变。
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<R<Void>> handleNotFound(Exception ex) {
        log.debug("路径不存在 traceId={} uri 见访问日志", MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(R.fail(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /** Sa-Token：未登录 / token 过期 → 2005，HTTP 401。 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLogin(NotLoginException ex) {
        // 不把 Sa-Token 的原始文案（如"token 无效"）透给客户端：它会暴露会话机制的细节
        log.debug("未登录 type={} traceId={}", ex.getType(), MDC.get(R.TRACE_ID_MDC_KEY));
        return ResponseEntity.status(ErrorCode.NOT_LOGGED_IN.getHttpStatus())
                .body(R.fail(ErrorCode.NOT_LOGGED_IN));
    }

    /** Sa-Token：角色/权限不足 → 1004，HTTP 403。 */
    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<R<Void>> handleNotPermission(Exception ex) {
        log.debug("权限不足 traceId={} message={}", MDC.get(R.TRACE_ID_MDC_KEY), ex.getMessage());
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(R.fail(ErrorCode.FORBIDDEN));
    }

    /**
     * 兜底。记 ERROR 并带完整堆栈与 traceId，<b>响应体里不回传任何异常细节</b>——
     * 数据库表名、SQL 片段、类名都可能从异常文案里泄漏出去，用户只需要一个 traceId
     * 用来向客服描述问题。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex) {
        log.error("未处理异常 traceId={}", MDC.get(R.TRACE_ID_MDC_KEY), ex);
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getHttpStatus())
                .body(R.fail(ErrorCode.SYSTEM_ERROR));
    }

    private FieldErrorVO toFieldError(FieldError fieldError) {
        return new FieldErrorVO(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private FieldErrorVO toFieldError(ConstraintViolation<?> violation) {
        // propertyPath 形如 "listSubjects.level"，取最后一段作为字段名，前端才能对到输入框
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return new FieldErrorVO(lastDot < 0 ? path : path.substring(lastDot + 1), violation.getMessage());
    }

    /**
     * 控制器方法参数校验失败 → 字段级错误。
     *
     * <p>字段名用 {@link #parameterName(MethodParameter)} 三级回退取，消息取第一条非空文案。
     */
    private FieldErrorVO toFieldError(ParameterValidationResult result) {
        String message = result.getResolvableErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("参数不合法");
        return new FieldErrorVO(parameterName(result.getMethodParameter()), message);
    }

    /**
     * 取参数名，三级回退——<b>顺序不能反</b>：
     *
     * <ol>
     *   <li><b>{@code @RequestParam} / {@code @PathVariable} 上显式声明的名字</b>。
     *       它来自接口契约本身，就是客户端在 URL 上用的那个名字（能绑定成功即证明二者一致），
     *       因此这是前端唯一能对上号的一级。</li>
     *   <li>{@code MethodParameter.getParameterName()}：Java 源码里的形参名。需要两个条件同时满足：
     *       编译期 {@code -parameters}（本项目<b>已</b>开启，{@code javap -v} 可见
     *       {@code MethodParameters} 属性）<b>且</b> {@code MethodParameter} 带有参数名发现器
     *       （Spring MVC 产出的带，本类测试里手搓的不带）。契约没写名字时（如
     *       {@code @RequestParam int page}）靠它拿到 {@code page}。</li>
     *   <li>{@code arg0} 索引名：兜底。宁可名字难看，也不能让字段名是 null——
     *       前端要靠它把错误挂到具体输入框上。</li>
     * </ol>
     *
     * <p>为什么第 1 级排在前面：{@code 1001} 的 {@code data} 是 {@code [{field, message}]}
     * （05 §5.1.2），前端按 {@code field} 定位输入框，而它认识的参数名是<b>自己发出去的那个</b>。
     * 两者通常在源码里写成同一个名字（{@code @RequestParam(name = "level") String level}），
     * 一旦有人写成 {@code @RequestParam(name = "pageSize") int page}，形参名 {@code page}
     * 对前端就是个陌生字段——实测确认过这条差异：把形参改名为 {@code lvl}、契约名仍为
     * {@code level} 后，容器返回的 {@code field} 是 {@code lvl}（即第 2 级先命中）。
     * 契约名优先，就把这个字段名的来源从"编译器参数"换成了"接口契约"——
     * 前者是构建选项，后者才是对外承诺。
     */
    private String parameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            String declared = !requestParam.name().isEmpty() ? requestParam.name() : requestParam.value();
            if (!declared.isEmpty()) {
                return declared;
            }
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null) {
            String declared = !pathVariable.name().isEmpty() ? pathVariable.name() : pathVariable.value();
            if (!declared.isEmpty()) {
                return declared;
            }
        }
        if (parameter.getParameterName() != null) {
            return parameter.getParameterName();
        }
        return "arg" + parameter.getParameterIndex();
    }
}

package com.imooc.user.exception;

import com.imooc.pojo.IMOOCJSONResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class CustomExceptionHandler {

    // 上传文件超过500kb，捕获异常：MaxUploadSizeExceededException
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public IMOOCJSONResult handleMaxUploadFile(MaxUploadSizeExceededException e) {
        return IMOOCJSONResult.errorMsg("文件上传大小不能超过500kb！");
    }
}

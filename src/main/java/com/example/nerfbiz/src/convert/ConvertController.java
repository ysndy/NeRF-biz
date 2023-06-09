package com.example.nerfbiz.src.convert;

import com.example.nerfbiz.config.BaseException;
import com.example.nerfbiz.config.BaseResponse;
import com.example.nerfbiz.config.Constant;
import com.example.nerfbiz.src.convert.model.*;
import com.example.nerfbiz.utils.JwtService;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.example.nerfbiz.config.BaseResponseStatus.INVALID_USER_JWT;

@CrossOrigin(origins = Constant.FRONT_SERVER_PATH)
@RestController
public class ConvertController {

    @Autowired
    private final ConvertProvider convertProvider;
    @Autowired
    private final ConvertService convertService;
    @Autowired
    private final JwtService jwtService;

    public ConvertController(ConvertProvider convertProvider, ConvertService convertService, JwtService jwtService) {
        this.convertProvider = convertProvider;
        this.convertService = convertService;
        this.jwtService = jwtService;
    }

    @RequestMapping(value = "video/{userIdx}", method = RequestMethod.GET)
    public BaseResponse<List<GetVideoRes>> getVideos(@PathVariable("userIdx") int userIdx){
        try {
            List<GetVideoRes> getVideoRes = convertProvider.getVideos(userIdx);
            return new BaseResponse<>(getVideoRes);
        }catch (BaseException exception){
            return new BaseResponse<>(exception.getStatus());
        }
    }

    @RequestMapping(value = "rendering/{userIdx}", method = RequestMethod.GET)
    public BaseResponse<List<GetRenderingRes>> getRenderings(@PathVariable("userIdx") int userIdx){
        try{
            List<GetRenderingRes> getRenderingRes = convertProvider.getRenderings(userIdx);
            return new BaseResponse<>(getRenderingRes);
        }catch (BaseException exception){
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /**
     *
     * @param userIdx
     * @param category
     * @param files
     * @return
     */

    @RequestMapping(value = "video", method = RequestMethod.POST)
    public BaseResponse<PostConvertRes> videoUpload(@RequestParam("userIdx") int userIdx, @RequestParam("category") String category, @RequestParam(value = "file", required = false) MultipartFile[] files) {

        //validation
        //userIdx 검증

//        try {
//            //int userIdxByJwt = jwtService.getUserIdx();
//            //if(userIdx != userIdxByJwt) return new BaseResponse<>(INVALID_USER_JWT);
//        }catch (BaseException exception){
//            return new BaseResponse<>(exception.getStatus());
//        }

        double duration = convertService.getVideoDuration(files[0]);

        if (duration >= 0) {
            System.out.println("동영상의 길이: " + duration + "초");
        } else {
            System.out.println("동영상의 길이를 가져올 수 없습니다.");
        }

        String objectId = System.currentTimeMillis() + "";
        String videoUrl;
        String objUrl;

        // gcs 업로드
        try {
            System.out.println("gcs 업로드");
            videoUrl = convertService.uploadVideo(userIdx, objectId, files[0]);

        } catch (BaseException exception) {
            exception.printStackTrace();
            return new BaseResponse(exception.getStatus());
        }

        // NeRF-server에 작업 요청
        try {
            System.out.println("NeRF-server에 작업 요청");
            objUrl = convertService.convert(category, objectId, videoUrl);

        } catch (BaseException exception) {
            exception.printStackTrace();
            return new BaseResponse<>(exception.getStatus());
        }

        return new BaseResponse<>(new PostConvertRes(videoUrl, Constant.RENDERING_SERVER_PATH+"?url="+objUrl));

    }


}

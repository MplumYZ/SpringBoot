package com.example.demo.controller;

import com.example.demo.config.Constant;
import com.example.demo.entity.*;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.*;
import com.example.demo.util.GetTimeNow;
import com.example.demo.util.JWTUtil;
import com.example.demo.util.Msg;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
@RestController
@RequestMapping({"/hr"})
public class HrController {

    @Autowired
    private UserService userService;
    @Autowired
    private WorkService workService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private EventService eventService;
    @Autowired
    private SigninService signinService;
    @Autowired
    private AchieveService achieveService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ApplyService applyService;
    @Autowired
    private UserMapper userMapper;

    public  int getIdbyToken(ServletRequest request){
        HttpServletRequest req = (HttpServletRequest) request;
        String token = req.getHeader("token");
        System.out.println("token"+token);
        String name= JWTUtil.getUsername(token);
        UserExample example=new UserExample();
        UserExample.Criteria criteria=example.createCriteria();
        criteria.andUsernameEqualTo(name);
        List<User> users=userMapper.selectByExample(example);
        System.out.println("user:"+users.get(0).toString());
        int userId = users.get(0).getId();
        System.out.println("id:"+userId);
        return  userId;
    }

    @ApiOperation(value = "??????????????????", notes = "?????????????????? hr master?????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/downloadUserResume")
    public Msg downloadUserResume(HttpServletResponse response , Model model, int userId) {
        User user=userService.getUserInfo(userId);
        String fileName=user.getResume();
        if(fileName==null)
            return Msg.fail().add("info", "?????????");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        byte[] buff = new byte[1024];
        //?????????????????????
        BufferedInputStream bis = null;
        OutputStream outputStream = null;

        try {
            outputStream = response.getOutputStream();

            //???????????????????????????????????????
            bis = new BufferedInputStream(new FileInputStream(new File(Constant.STATIC+ fileName )));
            int read = bis.read(buff);

            //??????while???????????????????????????????????????
            while (read != -1) {
                outputStream.write(buff, 0, buff.length);
                outputStream.flush();
                read = bis.read(buff);
            }
        } catch ( IOException e ) {
            e.printStackTrace();
            //??????????????????????????????????????????
            model.addAttribute("result","????????????");
            return Msg.fail().add("info", "????????????");
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //???????????????????????????
        model.addAttribute("result","????????????");
        return Msg.success().add("info", "????????????");

    }

    @ApiOperation(value = "????????????????????????", notes = "???????????????????????? hr master??????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/getUserWork")
    public Msg getUserWork(ServletRequest request , int userId){
        //int userId=this.getIdbyToken(request);
        List<Work> works=userService.getSelfWork(userId);
        User user=userService.getUserInfo(userId);
        for(Work w:works ){

            //w.setEvaluation(null);
            int comId=w.getCompanyid();
            Company company=companyService.getCompanyInfo(comId);
            w.setCompanyName(company.getCompanyname());

            User hr=userService.getUserInfo(w.getHrid());
            w.setHrName(hr.getUsername());
        }
        return Msg.success().add("works",works);
    }

    @ApiOperation(value = "??????????????????", notes = "???????????????????????????????????? ")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/getWorkEvent")
    public Msg getUserWorkEvent(ServletRequest request ,int workId){
        List<Event> events=eventService.getWorkEvent(workId);
        return Msg.success().add("events",events);
    }

    @ApiOperation(value = "??????????????????", notes = "????????????????????????????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/getWorkSign")
    public Msg getUserWorkSignin(ServletRequest request ,int workId){
        List<Signin> signins=signinService.getUserSignIn(workId);
        return Msg.success().add("signins",signins);
    }

    @ApiOperation(value = "??????????????????", notes = "?????????????????????????????? attend???0????????????")
    @PostMapping("/UserSignIn")
    public Msg UserSignIn(ServletRequest request,int userId){
        List<Signin> signins=signinService.getUserSignIn(userId);
        return Msg.success().add("signins",signins).add("allNum",signins.size()).add("attend",signinService.getAttend(userId));
    }

    @ApiOperation(value = "????????????", notes = "????????????????????????????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/getWorkAchieve")
    public Msg getUserWorkAchieve(ServletRequest request ,int workId){
        List<Achieve> achieves=achieveService.getWorkAchieve(workId);
        for(Achieve a:achieves){
            a.setTaskList(taskService.getAchieveTask(a.getAchieveid()));
        }
        return Msg.success().add("achieves",achieves);
    }

    @ApiOperation(value = "??????????????????????????????", notes = "????????????  0-????????? 1-?????? 2-??????")
    @PostMapping("/dimissionList")
    public Msg dimissionList(ServletRequest request){
        int userId=this.getIdbyToken(request);
        User user=userService.getUserInfo(userId);
        int comId=user.getCompanyid();
        if(!(comId>0))
            Msg.fail().add("info","????????????????????????");
        List<Apply> applies=applyService.getCompanyApply(comId,1);
        for(Apply apply:applies){
            apply.setUser(userService.getUserInfo(apply.getUserid()));
        }
        return Msg.success().add("applies",applies);
    }


    @ApiOperation(value = "??????????????????????????????", notes = "????????????  0-????????? 1-?????? 2-??????")
    @PostMapping("/jobList")
    public Msg jobList(ServletRequest request){
        int userId=this.getIdbyToken(request);
        User user=userService.getUserInfo(userId);
        int comId=user.getCompanyid();
        if(!(comId>0))
            Msg.fail().add("info","????????????????????????");
        List<Apply> applies=applyService.getCompanyApply(comId,2);
        for(Apply apply:applies){
            apply.setUser(userService.getUserInfo(apply.getUserid()));
        }
        return Msg.success().add("applies",applies);
    }

    @ApiOperation(value = "??????????????????", notes = "?????????????????? isAccept 1-?????? 2-??????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/demission")
    public Msg demission(ServletRequest request ,int applyId,int isAccept){
        int userId=this.getIdbyToken(request);
        Apply apply=applyService.getApply(applyId);
        apply.setIsaccept(isAccept);
        apply.setAcceptor(userId);
        applyService.update(apply);
        if(isAccept==1){
            User user=userService.getUserInfo(apply.getUserid());
            user.setCompanyid(null);
            userService.update(user);

            Work work=workService.getUserNewWork(apply.getUserid());

            work.setIsend(1);
            work.setHrid(userId);
            int y= GetTimeNow.getYear();
            int m=GetTimeNow.getMonth();
            int d=GetTimeNow.getDay();
            String today=y+"-"+m+"-"+d;
            work.setEndtime(today);
            workService.update(work);

            return  Msg.success().add("apply",apply).add("work",work);
        }

        return  Msg.success().add("apply",apply);
    }
    @ApiOperation(value = "??????????????????", notes = "???????????? isAccept 1-?????? 2-??????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/job")
    public Msg job(ServletRequest request ,int applyId,int isAccept){
        int userId=this.getIdbyToken(request);
        Apply apply=applyService.getApply(applyId);
        apply.setIsaccept(isAccept);
        apply.setAcceptor(userId);
        applyService.update(apply);
        if(isAccept==1){
            User user=userService.getUserInfo(apply.getUserid());

            Work worknow=workService.getUserNewWork(user.getId());
            if(worknow!=null)
                return  Msg.fail().add("info","???????????????????????????");

            user.setCompanyid(apply.getCompanyid());
            userService.update(user);

            Work work=new Work();
            work.setUseid(apply.getUserid());
            work.setCompanyid(apply.getCompanyid());
            work.setIsend(0);
            work.setHrid(userId);
            int y= GetTimeNow.getYear();
            int m=GetTimeNow.getMonth();
            int d=GetTimeNow.getDay();
            String today=y+"-"+m+"-"+d;
            work.setStarttime(today);

            workService.insert(work);
            return  Msg.success().add("apply",apply).add("work",work);
        }

        return  Msg.success().add("apply",apply);
    }
    @ApiOperation(value = "????????????", notes = "????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/setSignIn")
    public Msg setSignIn(ServletRequest request ,int y,int m,int d){
        String today=y+"-"+m+"-"+d;
        int userId=this.getIdbyToken(request);
        User user=userService.getUserInfo(userId);
        int comId=user.getCompanyid();
        List<User> users=companyService.getUser(comId);
        for(User u:users){
            Signin signin=new Signin();
            signin.setAttend(0);
            signin.setDaytime(today);
            signin.setWorkid(workService.getUserNewWork(u.getId()).getWorkid());
            signinService.insert(signin);
        }

        return  Msg.success().add("info","????????????");
    }

    @ApiOperation(value = "??????????????????????????????", notes = "status 400-???????????? 200- ?????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/checkSetSignIn")
    public Msg checkSetSignIn(ServletRequest request ){
        int y= GetTimeNow.getYear();
        int m=GetTimeNow.getMonth();
        int d=GetTimeNow.getDay();
        String today=y+"-"+m+"-"+d;
        int userId=this.getIdbyToken(request);
        User user=userService.getUserInfo(userId);
        int comId=user.getCompanyid();
        List<User> users=companyService.getUser(comId);
        int workId= workService.getUserNewWork(users.get(0).getId()).getWorkid();
        List<Signin> signins=signinService.seleteTodaySignIn(today,workId);
        if(signins.size()>0)
            return  Msg.fail().add("info","????????????");
        else
            return  Msg.success().add("info","????????????");
    }

    @ApiOperation(value = "?????????????????????", notes = "?????????????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/notSignIn")
    public Msg notSignIn(ServletRequest request ){
        int y= GetTimeNow.getYear();
        int m=GetTimeNow.getMonth();
        int d=GetTimeNow.getDay();
        String today=y+"-"+m+"-"+d;
        int userId=this.getIdbyToken(request);
        User user=userService.getUserInfo(userId);
        int comId=user.getCompanyid();
        List<User> users=companyService.getUser(comId);
        List<Integer> workIds= workService.getUsersWork(users);
        List<Signin> signins=signinService.seleteNotSignIn(today,workIds);
        for(Signin signin:signins){
            Work work=workService.getWorkInfo(signin.getWorkid());
            signin.setUser(userService.getUserInfo(work.getUseid()));
        }
        return Msg.success().add("signins",signins);
    }

    @ApiOperation(value = "?????????????????? ", notes = "??????????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/getUserInfo")
    public Msg getUserInfo(ServletRequest request ,int userId) {
        User user=userService.getUserInfo(userId);
        return Msg.success().add("user",user);
    }

    @ApiOperation(value = "??????????????????", notes = "??????????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/companyUser")
    public Msg companyUser(ServletRequest request ){
        int userId=this.getIdbyToken(request);
        User user=userService.getUserInfo(userId);
        int comId=user.getCompanyid();
        List<User> users=companyService.getUser(comId);
        return Msg.success().add("users",users);
    }

    @ApiOperation(value = "????????????hr", notes = "hr????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/companyHr")
    public Msg companyHr(ServletRequest request ,int workId){
        int userId=this.getIdbyToken(request);
        User user=userService.getUserInfo(userId);
        int comId=user.getCompanyid();
        List<User> users=companyService.getHr(comId);
        return Msg.success().add("users",users);
    }

    @ApiOperation(value = "??????????????????", notes = "???????????????????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/setEvent")
    public Msg setEvent(ServletRequest request ,int userId,String title,String info){
        Work work=workService.getUserNewWork(userId);
        Event event=new Event();
        event.setWorkid(work.getWorkid());
        event.setInfo(info);
        event.setTitle(title);
        eventService.insert(event);
        return Msg.success().add("event",event);
    }

    @ApiOperation(value = "????????????", notes = "?????????????????????")
    @RequiresRoles(logical = Logical.OR, value = {"hr", "master"})
    @PostMapping("/setEstimate")
    public Msg setEstimate(ServletRequest request ,int userId,String evaluation){
        Work work=workService.getUserNewWork(userId);
        work.setEvaluation(evaluation);
        workService.update(work);
        return Msg.success().add("work",work);
    }
}

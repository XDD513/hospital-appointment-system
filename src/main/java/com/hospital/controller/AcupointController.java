package com.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hospital.common.result.Result;
import com.hospital.entity.Acupoint;
import com.hospital.entity.AcupointCombination;
import com.hospital.service.AcupointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 穴位按摩指导控制器
 *
 * @author TCM Health Team
 * @date 2025-11-03
 */
@Slf4j
@RestController
@RequestMapping("/api/acupoint")
public class AcupointController {

    @Autowired
    private AcupointService acupointService;

    /**
     * 分页查询穴位列表
     *
     * @param meridian 经络（可选）
     * @param constitutionType 体质类型（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 穴位列表
     */
    @GetMapping("/list")
    public Result<IPage<Acupoint>> getAcupointList(
            @RequestParam(required = false) String meridian,
            @RequestParam(required = false) String constitutionType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("查询穴位列表：经络={}，体质={}，页码={}，每页={}条", meridian, constitutionType, pageNum, pageSize);
        return acupointService.getAcupointList(meridian, constitutionType, pageNum, pageSize);
    }

    /**
     * 获取穴位详情
     *
     * @param id 穴位ID
     * @return 穴位详情
     */
    @GetMapping("/detail/{id}")
    public Result<Acupoint> getAcupointDetail(@PathVariable Long id) {
        log.info("查询穴位详情：id={}", id);
        return acupointService.getAcupointDetail(id);
    }

    /**
     * 搜索穴位
     *
     * @param keyword 关键词
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 穴位列表
     */
    @GetMapping("/search")
    public Result<IPage<Acupoint>> searchAcupoints(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("搜索穴位：关键词={}，页码={}，每页={}条", keyword, pageNum, pageSize);
        return acupointService.searchAcupoints(keyword, pageNum, pageSize);
    }

    /**
     * 根据经络查询穴位
     *
     * @param meridian 经络名称
     * @return 穴位列表
     */
    @GetMapping("/meridian/{meridian}")
    public Result<List<Acupoint>> getAcupointsByMeridian(@PathVariable String meridian) {
        log.info("查询经络穴位：经络={}", meridian);
        return acupointService.getAcupointsByMeridian(meridian);
    }

    /**
     * 获取所有经络列表
     *
     * @return 经络列表
     */
    @GetMapping("/meridians")
    public Result<List<String>> getAllMeridians() {
        log.info("查询所有经络");
        return acupointService.getAllMeridians();
    }

    /**
     * 获取热门穴位
     *
     * @param limit 数量限制（默认10）
     * @return 穴位列表
     */
    @GetMapping("/popular")
    public Result<List<Acupoint>> getPopularAcupoints(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("查询热门穴位：limit={}", limit);
        return acupointService.getPopularAcupoints(limit);
    }

    /**
     * 分页查询穴位组合列表
     *
     * @param constitutionType 体质类型（可选）
     * @param symptom 症状（可选）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 穴位组合列表
     */
    @GetMapping("/combination/list")
    public Result<IPage<AcupointCombination>> getCombinationList(
            @RequestParam(required = false) String constitutionType,
            @RequestParam(required = false) String symptom,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("查询穴位组合列表：体质={}，症状={}，页码={}，每页={}条", constitutionType, symptom, pageNum, pageSize);
        return acupointService.getCombinationList(constitutionType, symptom, pageNum, pageSize);
    }

    /**
     * 获取穴位组合详情
     *
     * @param id 组合ID
     * @return 穴位组合详情
     */
    @GetMapping("/combination/{id}")
    public Result<AcupointCombination> getCombinationDetail(@PathVariable Long id) {
        log.info("查询穴位组合详情：id={}", id);
        return acupointService.getCombinationDetail(id);
    }

    /**
     * 根据用户体质推荐穴位组合
     *
     * @param userId 用户ID
     * @param limit 数量限制（默认5）
     * @return 推荐的穴位组合列表
     */
    @GetMapping("/combination/recommend")
    public Result<List<AcupointCombination>> getRecommendedCombinations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "5") Integer limit) {
        
        log.info("推荐穴位组合：用户ID={}，limit={}", userId, limit);
        return acupointService.getRecommendedCombinations(userId, limit);
    }

    /**
     * 搜索穴位组合
     *
     * @param keyword 关键词
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @return 穴位组合列表
     */
    @GetMapping("/combination/search")
    public Result<IPage<AcupointCombination>> searchCombinations(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        log.info("搜索穴位组合：关键词={}，页码={}，每页={}条", keyword, pageNum, pageSize);
        return acupointService.searchCombinations(keyword, pageNum, pageSize);
    }

    /**
     * 根据症状查询穴位组合
     *
     * @param symptom 症状
     * @return 穴位组合列表
     */
    @GetMapping("/combination/symptom/{symptom}")
    public Result<List<AcupointCombination>> getCombinationsBySymptom(@PathVariable String symptom) {
        log.info("查询症状穴位组合：症状={}", symptom);
        return acupointService.getCombinationsBySymptom(symptom);
    }

    /**
     * 获取热门穴位组合
     *
     * @param limit 数量限制（默认10）
     * @return 穴位组合列表
     */
    @GetMapping("/combination/popular")
    public Result<List<AcupointCombination>> getPopularCombinations(@RequestParam(defaultValue = "10") Integer limit) {
        log.info("查询热门穴位组合：limit={}", limit);
        return acupointService.getPopularCombinations(limit);
    }
}


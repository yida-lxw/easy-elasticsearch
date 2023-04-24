package cn.yzw.jc2.common.retry.enums;

/**
 * @Description: 优先级，从0开始
 * @Author: lbl 
 * @Date: 2023/4/23
 **/
public interface BizSequencePriorityEnum {
    /**
     * 顶层，不需要校验上一级
     */
    int TOP   = 0;
    int ONE   = 1;
    int TWO   = 2;
    int THREE = 3;
    int FOUR  = 4;
    int FIVE  = 5;
    int SIX   = 6;
    int SEVEN = 7;
    int EIGHT = 8;
    int NINE  = 9;
    int TEN   = 10;

}

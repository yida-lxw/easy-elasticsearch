package cn.yzw.jc2.common.util.mysql;

import lombok.Data;

import java.util.List;

/**
 * @Description: 需要更新的字段和表名
 * @Author: lbl
 * @Date:  2024/5/8 15:02
 * @param:
 * @return:
 **/
@Data
public class UpdateTableParams {
   /**
    * 需要更新的表名,必填
    */
   private String tableName;
   /**
    * 起始id，非必填，默认从0开始
    */
   private Long startId;

   /**
    * 需要更新的字段，必填
    */
   private List<UpdateField> fieldList;

   @Data
   static class UpdateField{
      /**
       * 目标字段，必填
       */
      private String targetFieldName;
      /**
       * 原字段，type=FIELD时，必填
       */
      private String originFieldName;
      /**
       * FIELD :字段，VALUE:值
       */
      private String UpdateFieldType;
      /**
       * 目标值：type=VALUE时，必填
       */
      private Object targetValue;

   }


}

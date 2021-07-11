package com.example.emos.wx.db.dao;


import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository //沒有mybatis可以用要自己造，代表和數據庫交流的層
public class MessageDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public String insert(MessageEntity entity) {
        Date sendTime = entity.getSendTime();
        sendTime = DateUtil.offset(sendTime, DateField.HOUR, 8); // 東八區，台北時間
        entity.setSendTime(sendTime);
        entity = mongoTemplate.save(entity);
        return entity.get_id(); // 返回mongodb為其插入的主鍵uuid
    }

    public List<HashMap> searchMessageByPage(int userId, long start, int length) {

        /** 原始操作語言
         * db.message.aggregate([
         //	{
         //		$set: {
         //			"id": { $toString: "$_id" }
         //		}
         //	},
         //	{
         //		$lookup:{
         //			from:"message_ref",
         //			localField:"id",
         //			foreignField:"messageId",
         //			as:"ref" #存成ref
         //		},
         //	},
         //	{ $match:{"ref.receiverId": 1} },
         //	{ $sort: {sendTime : -1} }, #按sendTime排成降序
         //	{ $skip: 0 }, #從哪開始
         //	{ $limit: 50 } #走多少步
         //])
         */
        // 結果
        // [
        //  {
        //    "_id": {"$oid": "600bea9ab5bafb311f147506"},
        //    "id": "600bea9ab5bafb311f147506",
        //    "msg": "HelloWorld",
        //    "ref": [ #個人刪除消息時，是把ref刪除掉
        //      {
        //        "_id": {"$oid": "600beaf0d6310000830036f3"},
        //        "messageId": "600bea9ab5bafb311f147506",
        //        "receiverId": 1,
        //        "readFlag": false,
        //        "lastFlag": true
        //      }
        //    ],
        //    "sendTime": {"$date": "2021-01-23T17:21:30.000Z"},
        //    "senderId": 0,
        //    "senderName": "Emos系统",
        //    "senderPhoto": "https://static-1258386385.cos.ap-beijing.myqcloud.com/img/System.jpg",
        //    "uuid": "bfcb7c47-5886-c528-5127-ce285bc2322a"
        //  }
        //]


        /**java代碼實現-start*/
        JSONObject json = new JSONObject();
        json.set("$toString", "$_id"); // 把_id的數據類型轉成String

        // 使用Aggregation進行兩個集合的聯合查詢
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.addFields().addField("id").withValue(json).build(), // 並把上面的json變成新的臨時變量id
                Aggregation.lookup("message_ref", "id", "messageId", "ref"),
                Aggregation.match(Criteria.where("ref.receiverId").is(userId)),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "sendTime")),
                Aggregation.skip(start),
                Aggregation.limit(length)
        );

        AggregationResults<HashMap> results = mongoTemplate.aggregate(aggregation, "message", HashMap.class);
        List<HashMap> list = results.getMappedResults(); // 提取成list
        /**java代碼實現-end*/

        list.forEach(one -> {
            List<MessageRefEntity> refList = (List<MessageRefEntity>) one.get("ref");
            MessageRefEntity entity = refList.get(0);

            boolean readFlag = entity.getReadFlag();
            String refId = entity.get_id();

            one.put("readFlag", readFlag);
            one.put("refId", refId);
            one.remove("ref"); // 把資料提出來，把原來藏有很多雜資料的ref刪掉
            one.remove("_id"); // 代表message的id，移動端沒必要持有，因為個人要刪除消息時是刪除ref

            // 時間轉換與顯示決定
            Date sendTime = (Date) one.get("sendTime");
            sendTime = DateUtil.offset(sendTime, DateField.HOUR, -8);

            String today = DateUtil.today();
            if (today.equals(DateUtil.date(sendTime).toDateStr())) { // 如果sendTime發生在今天，那就顯示時間就好不顯示日期
                one.put("sendTime", DateUtil.format(sendTime, "HH:mm"));
            } else {
                one.put("sendTime", DateUtil.format(sendTime, "yyyy/MM/dd"));
            }

        });
        return list;
    }

    public HashMap searchMessageById(String id){
        HashMap map = mongoTemplate.findById(id, HashMap.class, "message");
        Date sendTime = (Date) map.get("sendTime");
        sendTime = DateUtil.offset(sendTime, DateField.HOUR, -8);
        map.replace("sendTime", DateUtil.format(sendTime, "yyyy-MM-dd HH:mm"));
        return map;
    }

}

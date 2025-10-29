// service/community/CommentService.java
package service.community;

import dto.community.*;
import entity.Comment;
import java.sql.SQLException;

public interface CommentService {
    /**
     * 对帖子发表评论（一级评论）
     */
    PostCommentResponseDTO postComment(String contentId, PostCommentRequestDTO request) throws SQLException, IllegalArgumentException;
    
    /**
     * 回复评论（二级评论）
     */
    PostReplyResponseDTO postReply(String commentId, PostReplyRequestDTO request) throws SQLException, IllegalArgumentException;
    
    /**
     * 获取评论列表（包含楼中楼结构）
     */
    CommentListResponseDTO getCommentList(String contentId) throws SQLException;
    
    /**
     * 保存评论
     */
    void saveComment(Comment comment) throws SQLException;
    
    /**
     * 根据ID查找评论
     */
    Comment findCommentById(String commentId) throws SQLException;
}


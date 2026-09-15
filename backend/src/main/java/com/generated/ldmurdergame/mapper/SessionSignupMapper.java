package com.generated.ldmurdergame.mapper;

import com.generated.ldmurdergame.model.SessionSignup;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionSignupMapper {

  String COLUMNS = "id, session_id, player_name, status, seq, created_at, updated_at";

  @Insert("INSERT INTO session_signups (session_id, player_name, status, seq) "
      + "VALUES (#{sessionId}, #{playerName}, #{status}, #{seq})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(SessionSignup signup);

  @Select("SELECT " + COLUMNS + " FROM session_signups "
      + "WHERE session_id = #{sessionId} AND player_name = #{playerName}")
  SessionSignup findBySessionAndPlayer(@Param("sessionId") long sessionId, @Param("playerName") String playerName);

  @Select("SELECT COUNT(*) FROM session_signups WHERE session_id = #{sessionId} AND status = 'CONFIRMED'")
  int countConfirmed(@Param("sessionId") long sessionId);

  /** 场次内最大序号（含已取消记录），保证 seq 单调递增、候补顺序稳定。 */
  @Select("SELECT COALESCE(MAX(seq), 0) FROM session_signups WHERE session_id = #{sessionId}")
  long maxSeq(@Param("sessionId") long sessionId);

  @Select("SELECT " + COLUMNS + " FROM session_signups "
      + "WHERE session_id = #{sessionId} AND status = 'CONFIRMED' ORDER BY seq")
  List<SessionSignup> findConfirmed(@Param("sessionId") long sessionId);

  @Select("SELECT " + COLUMNS + " FROM session_signups "
      + "WHERE session_id = #{sessionId} AND status = 'WAITLIST' ORDER BY seq")
  List<SessionSignup> findWaitlist(@Param("sessionId") long sessionId);

  /** 候补队列中最早报名的一位，用于退位后的自动递补。 */
  @Select("SELECT " + COLUMNS + " FROM session_signups "
      + "WHERE session_id = #{sessionId} AND status = 'WAITLIST' ORDER BY seq LIMIT 1")
  SessionSignup firstWaiting(@Param("sessionId") long sessionId);

  @Update("UPDATE session_signups SET status = #{status}, seq = #{seq}, updated_at = CURRENT_TIMESTAMP "
      + "WHERE id = #{id}")
  int updateStatusAndSeq(@Param("id") long id, @Param("status") String status, @Param("seq") long seq);
}

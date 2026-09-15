package com.generated.ldmurdergame.mapper;

import com.generated.ldmurdergame.model.GameSession;
import com.generated.ldmurdergame.model.SessionRosterRow;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GameSessionMapper {

  @Insert("INSERT INTO game_sessions (title, session_date, time_slot, seat_count, status) "
      + "VALUES (#{title}, #{sessionDate}, #{timeSlot}, #{seatCount}, #{status})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(GameSession session);

  @Select("SELECT id, title, session_date, time_slot, seat_count, status, created_at, updated_at "
      + "FROM game_sessions WHERE id = #{id}")
  GameSession findById(@Param("id") long id);

  /** 行级排他锁：同一场次的报名/取消/递补/上下架全部串行化，保证不超卖、不重复占位。 */
  @Select("SELECT id, title, session_date, time_slot, seat_count, status, created_at, updated_at "
      + "FROM game_sessions WHERE id = #{id} FOR UPDATE")
  GameSession lockById(@Param("id") long id);

  /**
   * 单条 SQL 联表取回场次与有效报名：一条语句读取同一时点快照，
   * 保证响应中的座位数、确认名单、候补队列、余位与状态互相自洽。
   */
  String ROSTER_SELECT = "SELECT s.id AS session_id, s.title, s.session_date, s.time_slot, s.seat_count, "
      + "s.status AS session_status, u.id AS signup_id, u.player_name, u.status AS signup_status, "
      + "u.seq AS signup_seq, u.created_at AS signup_created_at "
      + "FROM game_sessions s LEFT JOIN session_signups u "
      + "ON u.session_id = s.id AND u.status IN ('CONFIRMED', 'WAITLIST') ";
  String ROSTER_ORDER = " ORDER BY s.time_slot, s.id, CASE WHEN u.status = 'CONFIRMED' THEN 0 ELSE 1 END, u.seq";

  @Select(ROSTER_SELECT + "WHERE s.session_date = #{date}" + ROSTER_ORDER)
  List<SessionRosterRow> findRosterByDate(@Param("date") LocalDate date);

  @Select(ROSTER_SELECT + "WHERE s.id = #{id}" + ROSTER_ORDER)
  List<SessionRosterRow> findRosterById(@Param("id") long id);

  @Update("UPDATE game_sessions SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  int updateStatus(@Param("id") long id, @Param("status") String status);

  @Update("UPDATE game_sessions SET seat_count = #{seatCount}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  int updateSeatCount(@Param("id") long id, @Param("seatCount") int seatCount);
}

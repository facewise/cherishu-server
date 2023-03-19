package cherish.backend.board.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public class MonthlyBoard {

    @Id @GeneratedValue
    private String id;
    private String monthlyDate; // 공지 날짜
    private String title; // 제목
    private String subtitle; //부 제목

}

package cherish.backend.item.model;

import cherish.backend.common.model.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@ToString
public class Item extends BaseTimeEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String brand;
    private String name;
    private String description;
    private int price;
    private int views;
    private String imgUrl;
    private int minAge;
    private int maxAge;
}

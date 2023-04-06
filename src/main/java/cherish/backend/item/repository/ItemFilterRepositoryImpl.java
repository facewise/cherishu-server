package cherish.backend.item.repository;

import cherish.backend.category.model.QCategory;
import cherish.backend.common.config.QueryDslConfig;
import cherish.backend.item.dto.*;
import cherish.backend.item.model.*;
import cherish.backend.member.model.QJob;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static cherish.backend.category.model.QCategory.*;
import static cherish.backend.category.model.QFilter.*;
import static cherish.backend.item.model.QItem.*;
import static cherish.backend.item.model.QItemCategory.*;
import static cherish.backend.item.model.QItemFilter.*;
import static cherish.backend.item.model.QItemJob.itemJob;
import static cherish.backend.item.model.QItemUrl.*;
import static cherish.backend.member.model.QJob.job;
import static com.querydsl.core.group.GroupBy.groupBy;
import static org.springframework.util.StringUtils.*;

@RequiredArgsConstructor
public class ItemFilterRepositoryImpl implements ItemFilterRepositoryCustom{

    private final QueryDslConfig queryDslConfig;

    @Override
    public List<ItemFilterQueryDto> findItemFilterByNameAndId(ItemFilterCondition filterCondition) {
        return queryDslConfig.jpaQueryFactory()
                .select(new QItemFilterQueryDto(
                        item.id.as("itemId"),
                        filter.id.as("filterId"),
                        itemFilter.id.as("itemFilterId"),
                        item.name.as("itemName"),
                        filter.name.as("filterName"),
                        itemFilter.name.as("itemFilterName"),
                        item.price.as("itemPrice")
                ))
                .from(itemFilter)
                .join(itemFilter.filter, filter)
                .where(
                        itemFilterNameEq(filterCondition.getItemFilterName()),
                        filterIdEq(filterCondition.getFilterId()))
                .fetch();
    }

    @Override
    public List<AgeFilterQueryDto> findItemFilterByAge(AgeFilterCondition ageCondition) {
        return queryDslConfig.jpaQueryFactory()
                .select(new QAgeFilterQueryDto(
                        item.id.as("itemId"),
                        filter.id.as("filterId"),
                        itemFilter.id.as("itemFilterId"),
                        item.name.as("itemName"),
                        filter.name.as("filterName"),
                        itemFilter.name.as("itemFilterName"),
                        item.minAge.as("minAge"),
                        item.maxAge.as("maxAge")
                ))
                .from(itemFilter)
                .join(itemFilter.filter, filter)
                .where(
                        itemFilterNameEq(ageCondition.getItemFilterName()),
                        filterIdEq(ageCondition.getFilterId()),
                        ageGoe(ageCondition.getAgeGoe()),
                        ageLoe(ageCondition.getAgeLoe()))
                .fetch();
    }

    @Override
    public Page<ItemSearchResponseDto.ResponseSearchItem> searchItem(ItemSearchCondition searchCondition, Pageable pageable) {
        List<ItemSearchResponseDto.ResponseSearchItem> content = queryDslConfig.jpaQueryFactory()
                .select(new QItemSearchResponseDto_ResponseSearchItem(
                        new QItemSearchResponseDto_FilterDto(filter.id, filter.name),
                        new QItemSearchResponseDto_FilterDto(itemFilter.id, itemFilter.name),
                        new QItemSearchResponseDto_ItemDto(item.id, item.name, item.brand, item.description, item.price, item.imgUrl, item.minAge, item.maxAge),
                        new QItemSearchResponseDto_CategoryDto(category.id, category.name, findParent(category), findChild(category)),
                        new QItemSearchResponseDto_CategoryDto(itemCategory.id, itemCategory.category.name, findParent(itemCategory.category), findChild(itemCategory.category)),
                        new QItemSearchResponseDto_JobDto(job.id, job.name, findJobParent(job), findJobChild(job)),
                        new QItemSearchResponseDto_JobDto(itemJob.id, itemJob.job.name, findJobParent(itemJob.job), findJobChild(itemJob.job)),
                        new QItemSearchResponseDto_ItemUrlDto(itemUrl.id, itemUrl.url, itemUrl.platform)
                ))
                .from(item)
                .leftJoin(item.itemFilters, itemFilter)
                .leftJoin(item.itemJobs, itemJob)
                .leftJoin(item.itemCategories, itemCategory)
                .leftJoin(itemUrl).on(itemUrl.item.id.eq(item.id))
                .leftJoin(itemFilter.filter, filter)
                .leftJoin(itemJob.job, job)
                .leftJoin(itemCategory.category, category)
                .where(getSearchCondition(searchCondition))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Item> countQuery = queryDslConfig.jpaQueryFactory()
                .selectFrom(item)
                .leftJoin(item.itemFilters, itemFilter)
                .leftJoin(item.itemJobs, itemJob)
                .leftJoin(item.itemCategories, itemCategory)
                .leftJoin(itemUrl).on(itemUrl.item.id.eq(item.id))
                .leftJoin(itemFilter.filter, filter)
                .leftJoin(itemJob.job, job)
                .leftJoin(itemCategory.category, category)
                .where(getSearchCondition(searchCondition))
                .distinct()
                .select(item);

        long total = countQuery.fetch().size();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanBuilder getSearchCondition(ItemSearchCondition searchCondition) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(itemNameEq(searchCondition.getItemName()));
        builder.and(itemBrandEq(searchCondition.getItemBrand()));
        builder.and(keywordEq(searchCondition.getKeyword(), searchCondition.getTarget()));
        builder.and(categoryParentEq(searchCondition.getItemCategoryParent()));
        builder.and(categoryChildrenEq(searchCondition.getItemCategoryChildren()));
        builder.and(jobParentEq(searchCondition.getItemJobParent()));
        builder.and(jobChildrenEq(searchCondition.getItemJobChildren()));
        builder.and(itemFilterNameEq(searchCondition.getItemFilterName()));
        builder.and(filterNameEq(searchCondition.getFilterName()));
        return builder;
    }

    private BooleanExpression categoryParentEq(String categoryParent) {
        return StringUtils.hasText(categoryParent) ? category.parent.name.eq(categoryParent) : null;
    }

    private BooleanExpression categoryChildrenEq(List<String> categoryChildren) {
        return !CollectionUtils.isEmpty(categoryChildren) ? category.children.any().name.in(categoryChildren) : null;
    }

    private BooleanExpression jobParentEq(String jobParent) {
        return StringUtils.hasText(jobParent) ? job.parent.name.eq(jobParent) : null;
    }

    private BooleanExpression jobChildrenEq(List<String> jobChildren) {
        return !CollectionUtils.isEmpty(jobChildren) ? job.children.any().name.in(jobChildren) : null;
    }

    private BooleanExpression itemNameEq(String itemName) {
        return StringUtils.hasText(itemName) ? item.name.eq(itemName) : null;
    }

    private BooleanExpression itemFilterNameEq(String itemFilterName) {
        return hasText(itemFilterName) ? itemFilter.name.eq(itemFilterName) : null;
    }

    private BooleanExpression itemBrandEq(String itemBrand) {
        return hasText(itemBrand) ? item.brand.eq(itemBrand) : null;
    }

    private BooleanExpression filterIdEq(Long filterId) {
        return hasText(String.valueOf(filterId)) ? filter.id.eq(filterId) : null;
    }

    private BooleanExpression filterNameEq(String filterName) {
        return hasText(filterName) ? filter.name.eq(filterName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? item.maxAge.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? item.minAge.loe(ageLoe) : null;
    }

    private BooleanExpression keywordEq(String keyword, String target) {
        return keyword != null ? targetEqWithKeyword(keyword, target) : null;
    }

    private BooleanExpression targetEqWithKeyword(String keyword, String target) {
        if (target == null) {
            return null;
        }
        switch (target) {
            case "filterName":
                return filter.name.contains(keyword);
            case "itemFilterName":
                return itemFilter.name.contains(keyword);
            case "categoryParent":
                return category.parent.name.contains(keyword);
            case "categoryChildren":
                return category.children.any().name.contains(keyword);
            case "jobParent":
                return job.parent.name.contains(keyword);
            case "jobChildren":
                return job.children.any().name.contains(keyword);
            case "itemName":
                return item.name.contains(keyword);
            case "itemBrand":
                return item.brand.contains(keyword);
            default:
                throw new IllegalArgumentException("Invalid target: " + target);
        }
    }

    private Expression findParent(QCategory category) {
        QCategory qCateParent = new QCategory("cateParent");

        return queryDslConfig.jpaQueryFactory()
                .select(qCateParent)
                .from(category)
                .join(category.parent, qCateParent)
                .where(qCateParent.children.contains(category));
    }

    private Expression findChild(QCategory category) {
        QCategory qCateChild = new QCategory("qCategory");

        return queryDslConfig.jpaQueryFactory()
                .select(Expressions.asString(qCateChild.name).as("cateChild"))
                .from(qCateChild)
                .where(qCateChild.parent.eq(category))
                .fetchJoin();
    }

    private Expression findJobParent(QJob job) {
        QJob jParent = new QJob("jParent");

        return queryDslConfig.jpaQueryFactory()
                .select(jParent)
                .from(job)
                .join(job.parent, jParent)
                .where(jParent.children.contains(job));
    }

    private Expression findJobChild(QJob job) {
        QJob jChildren = new QJob("jChildren");

        return queryDslConfig.jpaQueryFactory()
                .select(Expressions.asString(jChildren.name).as("cateChild"))
                .from(jChildren)
                .join(jChildren.parent, jChildren)
                .where(jChildren.parent.eq(job));
    }
}

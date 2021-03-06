package org.grape;

import com.google.common.base.Strings;
import io.ebean.PagedList;
import io.ebean.Query;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public abstract class BaseCrudService<T extends BaseDomain> implements CrudService<T> {
    protected final BaseDomain.Finder<T> finder;
    protected final Class<T> domainClass;

    public BaseCrudService(Class<T> domainClass) {
        this.domainClass = domainClass;
        finder = new BaseDomain.Finder<>(domainClass);
    }

    @Override
    public void insert(T domain) {
        /**
         * 雪花算法主键生成
         */
        if (domain.getId() == null) {
            domain.setId(SpringUtil.getBean(SnowflakeIdWorker.class).nextId());
        }
        domain.insert();
    }

    @Override
    public void update(T domain) {
        domain.update();
    }

    @Override
    public void save(T domain) {
        domain.save();
    }

    @Override
    public void insertOrUpdate(T domain) {
        if (domain.getId() == null) {
            insert(domain);
        } else {
            Optional<T> optional = this.findById(domain.getId());
            if (optional.isPresent()) {
                this.update(domain);
            } else {
                this.insert(domain);
            }
        }
    }

    @Override
    public void insertOrUpdateByIdIsNull(T domain) {
        if (domain.getId() == null) {
            insert(domain);
        } else {
            update(domain);
        }
    }

    @Override
    public boolean delete(T domain) {
        return domain.delete();
    }

    @Override
    public boolean deletePermanent(T domain) {
        return domain.deletePermanent();
    }

    @Override
    public void deleteById(Long id) {
        finder.deleteById(id);
    }

    @Override
    public List<T> all() {
        return finder.all();
    }

    @Override
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(findByIdNullable(id));
    }

    @Nullable
    @Override
    public T findByIdNullable(Long id) {
        return finder.byId(id);
    }

    @NonNull
    @Override
    public T findByIdNonNull(Long id) {
        T t = finder.byId(id);
        if (t == null) {
            throw new GrapeException(String.format("can not found %s by id %s", domainClass.getName(), id));
        } else {
            return t;
        }
    }

    @Override
    public PagedResultList<T> find(SimpleQuery cond) {
        Query<T> query = finder.query();
        PagedList<T> pagedList = query
                .select(String.join(",", cond.getColumns()))
                .where(cond.whereExpression(query))
                .setOrderBy(cond.orderBy())
                .setMaxRows(cond.getPageSize())
                .setFirstRow(cond.offSet())
                .findPagedList();
        return new PagedResultList<T>(pagedList);
    }
}

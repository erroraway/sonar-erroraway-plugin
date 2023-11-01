package application;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;

/**
 * Hibernate JPA metamodel annotation processing sample
 */
@Entity
public class HibernateEntity {
	@Id
	private Long id;
	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		Duration.of(1, ChronoUnit.YEARS);
		System.out.println(findByName(null, name));
		
		this.name = name;
	}

	public static HibernateEntity findByName(EntityManager entityManager, String name) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<HibernateEntity> cq = cb.createQuery(HibernateEntity.class);
		Root<HibernateEntity> root = cq.from(HibernateEntity.class);
		
		cq.select(root).where(cb.equal(root.get(HibernateEntity_.name), name));
		
		Query query = entityManager.createQuery(cq);

		return (HibernateEntity) query.getSingleResult();
	}
}

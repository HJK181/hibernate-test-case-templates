package org.hibernate.bugs;

import jakarta.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
class JPAUnitTestCase {

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void init() {
		entityManagerFactory = Persistence.createEntityManagerFactory( "templatePU" );
	}

	@AfterEach
	void destroy() {
		entityManagerFactory.close();
	}

	// Entities are auto-discovered, so just add them anywhere on class-path
	// Add your tests, using standard JUnit.
	@Test
	void hhh19724Test() throws Exception {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.getTransaction().begin();

		var a = new A();
		entityManager.persist(a);

		var c = new C();
		entityManager.persist(c);

		var b = new B();
		b.setC(c);
		b.setA(a);
		entityManager.persist(b);

		c.setB(b); // FIXME: this causes org.hibernate.TransientObjectException
		entityManager.merge(c);

		a.addB(b);
		entityManager.merge(a);

		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();

		var a1 = entityManager.find(A.class, 1L);
		a1.removeB(a1.getBs().iterator().next());

		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Entity
	static class A {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(nullable = false)
		private int variantCount;

		@OneToMany(mappedBy = "a", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<B> bs = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public boolean removeB(B b) {
			var removed = bs.remove(b);
			b.setA(null);
			b.getC().setB(null);
			variantCount -= 1;
			return removed;
		}

		public boolean addB(B b) {
			var added = bs.add(b);
			b.setA(this);
			variantCount +=1;
			return added;
		}

		public Set<B> getBs() {
			return bs;
		}

		public void setBs(Set<B> bs) {
			this.bs = bs;
		}

		public int getVariantCount() {
			return variantCount;
		}

		public void setVariantCount(int variantCount) {
			this.variantCount = variantCount;
		}
	}

	@Entity
	static class B {

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		@MapsId
		private C c;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(nullable = false)
		private A a;

		public A getA() {
			return a;
		}

		public void setA(A a) {
			this.a = a;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public C getC() {
			return c;
		}

		public void setC(C c) {
			this.c = c;
		}
	}

	@Entity
	static class C {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@OneToOne(mappedBy = "c") // FIXME: problematic bidirectional mapping
		private B b;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public B getB() {
			return b;
		}

		public void setB(B b) {
			this.b = b;
		}
	}
}

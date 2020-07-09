/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernare.example;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.junit.Before;
import org.junit.Test;

import org.hibernare.utils.AbstractAllowEnhancementAsProxyTest;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * @author Andrea Boriero
 */
public class AllowEnhancementAsProxyTestWithpolymorphicAssociationTest extends AbstractAllowEnhancementAsProxyTest {


	@Test
	public void polymorphycAssociationTest() {
		StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		inTransaction(
				session -> {
					Order order = session.load( Order.class, 1 );
					assertThat( order, is( instanceOf( PersistentAttributeInterceptable.class ) ) );
					assertThat(
							( (PersistentAttributeInterceptable) order ).$$_hibernate_getInterceptor(),
							is( instanceOf(
									EnhancementAsProxyLazinessInterceptor.class ) )
					);
					assertFalse( Hibernate.isPropertyInitialized( order, "customer" ) );
					Customer customer = order.getCustomer(); // is an instance of HibernateProxy, the @LazyToOne(LazyToOneOption.NO_PROXY) is not taken into consideration, due to the inheritance we cannot instantiate an Enhanced proxy so if we do not use an Hibernate proxy an extra query has to be executed to detect the concrete class.
					assertThat( customer, is( instanceOf( HibernateProxy.class ) ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Order order = new Order( 1 );
					DomesticCustomer domesticCustomer = new DomesticCustomer( 2 );
					order.setCustomer( domesticCustomer );

					session.save( domesticCustomer );
					session.save( order );
				}
		);
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		sources.addAnnotatedClass( Order.class );
		sources.addAnnotatedClass( Customer.class );
		sources.addAnnotatedClass( DomesticCustomer.class );
		sources.addAnnotatedClass( ForeignCustomer.class );
	}

	@Entity
	public static class Order {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@JoinColumn
		private Customer customer;

		public Order() {
		}

		public Order(Integer id) {
			this.id = id;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	@Entity
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class Customer {
		@Id
		private Integer id;

		public Customer() {

		}

		public Customer(Integer id) {
			this.id = id;
		}
	}

	@Entity
	public static class DomesticCustomer extends Customer {
		private String name;

		public DomesticCustomer() {
		}

		public DomesticCustomer(Integer id) {
			super( id );
		}
	}

	@Entity
	public static class ForeignCustomer extends Customer {
		private String name;

		public ForeignCustomer() {
		}

		public ForeignCustomer(Integer id) {
			super( id );
		}
	}


}

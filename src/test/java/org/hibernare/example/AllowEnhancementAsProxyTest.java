/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernare.example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.CoreMatchers;
import org.hibernare.utils.AbstractAllowEnhancementAsProxyTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */

public class AllowEnhancementAsProxyTest extends AbstractAllowEnhancementAsProxyTest {

	@Test
	public void getTheEntityAssociationOwnerTest() {
		StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		inTransaction(
				session -> {
					/*
					table AEntity (A_ID bigint not null, aString varchar(255), primary key (A_ID))
					table AEntity (A_ID bigint not null, aString varchar(255), primary key (A_ID))

					BEntity contains the Fk so we DO NOT need @LazyToOne(LazyToOneOption.NO_PROXY)
					on its @OneToOne in order to avoid an extra query (to create a Enhanced proxy for AEntity we need its ID that is a_A_ID).
				 	*/
					Address address = session.get( Address.class, 2L );
					/*
						select example1te0_.B_ID as b_id1_1_0_, example1te0_.a_A_ID as a_a_id2_1_0_ from BEntity example1te0_ where example1te0_.B_ID=?
					 */
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 1L ) );

					assertIsEnhancedInstance( address );

					// Being AEntity an EnhancedProxy it results as initialized for the LazyAttributeLoadingInterceptor of
					// the bEntity enhanced class but only the id value is initialized
					assertTrue( Hibernate.isPropertyInitialized( address, "user" ) );

					User user = address.getUser();
					assertIsEnhancedAsProxyInstance( user );

					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 1L ) );
					// accessing the id attribute of an Enhanced proxy does not trigger any query
					assertThat( user.getId(), is( 1L ) );
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 1L ) );

					assertThat( user.getName(), is( "Fab" ) );
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 2L ) );
				}
		);
	}

	@Test
	public void loadTheEntityAssociationOwnerTest() {
		StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		inTransaction(
				session -> {
					/*
					table AEntity (A_ID bigint not null, aString varchar(255), primary key (A_ID))
					table BEntity (B_ID bigint not null, aString varchar(255), a_A_ID bigint, primary key (B_ID))

					BEntity contains the Fk so we DO NOT need @LazyToOne(LazyToOneOption.NO_PROXY)
					on its @OneToOne in order to avoid an extra query (to create a Enhanced proxy for AEntity we need its ID that is a_A_ID).
				 	*/
					Address address = session.load( Address.class, 2L );
					// no query is performed and an Enhanced Proxy is created
					assertThat( address.getId(), is( 2L ) );
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 0L ) );

					assertIsEnhancedAsProxyInstance( address );

					assertFalse( Hibernate.isPropertyInitialized( address, "user" ) );

					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 0L ) );

					// Accessing any attribute of the Enhanced Proxy that is not its id trigger a query to initialized it
					User user = address.getUser();
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 1L ) );
					assertIsEnhancedAsProxyInstance( user );

					// accessing the id attribute of an Enhanced proxy does not trigger any query
					assertThat( user.getId(), is( 1L ) );
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 1L ) );

					assertThat( user.getName(), is( "Fab" ) );
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 2L ) );

				}
		);
	}

	@Test
	public void getTheEntityNotAssociationOwnerTest() {
		StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		inTransaction(
				session -> {
					/*
					table AEntity (A_ID bigint not null, aString varchar(255), primary key (A_ID))
					table BEntity (B_ID bigint not null, aString varchar(255), a_A_ID bigint, primary key (B_ID))

					AEntity table does not contains the Fk so we need @LazyToOne(LazyToOneOption.NO_PROXY)
					on its @OneToOne in order to avoid an extra query (to create a Enhanced proxy for BEntity we need its ID).
				 	*/
					User user = session.get( User.class, 1L );
					assertIsEnhancedInstance( user );
					assertFalse( Hibernate.isPropertyInitialized( user, "address" ) );

					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 1L ) );
				}
		);
	}

	@Test
	public void loadTheEntityNotAssociationOwnerTest() {
		StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();
		inTransaction(
				session -> {
					/*
					table AEntity (A_ID bigint not null, aString varchar(255), primary key (A_ID))
					table BEntity (B_ID bigint not null, aString varchar(255), a_A_ID bigint, primary key (B_ID))

					AEntity table does not contains the Fk so we need @LazyToOne(LazyToOneOption.NO_PROXY)
					on its @OneToOne in order to avoid an extra query (to create a Enhanced proxy for BEntity we need its ID).
				 	*/
					User user = session.load( User.class, 1L );
					assertIsEnhancedAsProxyInstance( user );
					assertThat( statistics.getPrepareStatementCount(), CoreMatchers.is( 0L ) );

					assertFalse( Hibernate.isPropertyInitialized( user, "b" ) );

				}
		);
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					User a = new User( 1L, "Fab" );
					Address b = new Address( 2L, "Sancrofd St", "SE11 %UG", "London" );

					a.setAddress( b );
					b.setUser( a );
					session.save( a );
					session.save( b );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Address" ).executeUpdate();
					session.createQuery( "delete from User" ).executeUpdate();
				}
		);
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		sources.addAnnotatedClass( User.class );
		sources.addAnnotatedClass( Address.class );
	}

	@Table(name = "User")
	@Entity(name = "User")
	public static class User {

		@Id
		@Column(name = "A_ID")
		Long id;

		String name;

		public User() {
		}

		public User(Long id) {
			this.id = id;
		}

		public User(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
		//without LazyToOneOption.NO_PROXY 2 queries will be executed, the second one in order to retrieve the id needed for the Hibernate Proxy initialization
		@LazyToOne(LazyToOneOption.NO_PROXY)
		Address address;

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}
	}

	@Table(name = "Address")
	@Entity(name = "Address")
	public static class Address {

		@Id
		@Column(name = "B_ID")
		Long id;

		String street;
		String zipcode;
		String city;

		public Address() {
		}

		public Address(Long id) {
			this.id = id;
		}

		public Address(Long id, String street, String zipcode, String city) {
			this.id = id;
			this.street = street;
			this.zipcode = zipcode;
			this.city = city;
		}

		@OneToOne(fetch = FetchType.LAZY)
		private User user;

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public Long getId() {
			return id;
		}

		public String getStreet() {
			return street;
		}

		public String getZipcode() {
			return zipcode;
		}

		public String getCity() {
			return city;
		}
	}

}

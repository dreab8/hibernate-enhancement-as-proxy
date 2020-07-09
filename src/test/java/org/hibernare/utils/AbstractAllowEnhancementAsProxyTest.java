package org.hibernare.utils;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ EnhancementContext.class })
public abstract class AbstractAllowEnhancementAsProxyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	protected void assertIsEnhancedAsProxyInstance(Object entity) {
		assertThat( entity, is( instanceOf( PersistentAttributeInterceptable.class ) ) );
		assertThat( ( (PersistentAttributeInterceptable) entity ).$$_hibernate_getInterceptor(), is( instanceOf(
				EnhancementAsProxyLazinessInterceptor.class ) ) );
	}

	protected void assertIsEnhancedInstance(Object entity) {
		assertThat( entity, is( instanceOf( PersistentAttributeInterceptable.class ) ) );
		assertThat( ( (PersistentAttributeInterceptable) entity ).$$_hibernate_getInterceptor(), is( instanceOf(
				LazyAttributeLoadingInterceptor.class ) ) );
	}

}

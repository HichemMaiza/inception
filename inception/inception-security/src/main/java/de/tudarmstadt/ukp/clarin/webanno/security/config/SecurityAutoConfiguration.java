/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.security.config;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.transaction.PlatformTransactionManager;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;

@Configuration
@EnableConfigurationProperties(SecurityPropertiesImpl.class)
public class SecurityAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;
    private @Autowired(required = false) PlatformTransactionManager transactionManager;

    @Bean("userRepository")
    public UserDao userService(SecurityProperties aSecurityProperties,
            @Autowired(required = false) SessionRegistry aSessionRegistry)
    {
        return new UserDaoImpl(entityManager, aSecurityProperties, transactionManager,
                aSessionRegistry);
    }
}

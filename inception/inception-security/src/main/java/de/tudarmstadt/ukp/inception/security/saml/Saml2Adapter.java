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
package de.tudarmstadt.ukp.inception.security.saml;

import java.util.Map;

import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider.ResponseToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface Saml2Adapter
{
    Map<String, String> getSamlRelyingPartyRegistrations();

    Saml2Authentication process(ResponseToken aToken, Saml2Authentication aAuthentication);

    Saml2Authentication process(org.springframework.security.saml2.provider.service. //
            authentication.OpenSaml4AuthenticationProvider.ResponseToken aToken,
            Saml2Authentication aAuthentication);

    User loadSamlUser(String aUsername, String aRegistrationId);
}

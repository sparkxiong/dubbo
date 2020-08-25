/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.RegistryProtocol;

import static org.apache.dubbo.common.constants.RegistryConstants.INIT;

@Activate
public class MigrationRuleListener<T> {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleListener.class);
    private static final String DUBBO_SERVICEDISCOVERY_MIGRATION = "dubbo.application.service-discovery.migration";

    private MigrationInvoker<T> migrationInvoker;

    public MigrationRuleListener(MigrationInvoker<T> invoker) {
        this.migrationInvoker = invoker;
    }

    public void doMigrate(String rawRule) {
        MigrationStep step = (migrationInvoker instanceof RegistryProtocol.ServiceDiscoveryMigrationInvoker)
                ? MigrationStep.FORCE_APPLICATION
                : MigrationStep.INTERFACE_FIRST;
        if (StringUtils.isEmpty(rawRule)) {
            logger.error("Find empty migration rule, will ignore.");
            return;
        } else if (INIT.equals(rawRule)) {
            step = Enum.valueOf(MigrationStep.class, ConfigurationUtils.getProperty(DUBBO_SERVICEDISCOVERY_MIGRATION, step.name()));
        } else {
            MigrationRule rule = MigrationRule.parse(rawRule);
            step = rule.getStep();
        }

        switch (step) {
            case APPLICATION_FIRST:
                migrationInvoker.migrateToServiceDiscoveryInvoker(false);
                break;
            case FORCE_APPLICATION:
                migrationInvoker.migrateToServiceDiscoveryInvoker(true);
                break;
            case INTERFACE_FIRST:
            default:
                migrationInvoker.fallbackToInterfaceInvoker();
        }
    }
}
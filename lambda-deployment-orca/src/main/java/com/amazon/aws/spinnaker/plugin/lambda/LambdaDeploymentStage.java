/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.amazon.aws.spinnaker.plugin.lambda;

import com.amazon.aws.spinnaker.plugin.lambda.eventconfig.LambdaUpdateEventConfigurationTask;
import com.amazon.aws.spinnaker.plugin.lambda.traffic.LambdaWaitToStabilizeTask;
import com.amazon.aws.spinnaker.plugin.lambda.upsert.*;
import com.amazon.aws.spinnaker.plugin.lambda.verify.LambdaCacheRefreshTask;
import com.amazon.aws.spinnaker.plugin.lambda.verify.LambdaVerificationTask;
import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
@StageDefinitionBuilder.Aliases({"Aws.LambdaDeploymentStage"})
public class LambdaDeploymentStage implements StageDefinitionBuilder {
    private static Logger logger = LoggerFactory.getLogger(LambdaDeploymentStage.class);

    public LambdaDeploymentStage() {
        logger.debug("Constructing Aws.LambdaDeploymentStage");
    }

    @Override
    public void taskGraph(@Nonnull StageExecution stage, @Nonnull TaskNode.Builder builder) {
        logger.debug("taskGraph for Aws.LambdaDeploymentStage");
        builder.withTask("lambdaCacheRefreshTask", LambdaCacheRefreshTask.class);
        builder.withTask("lambdaCreateTask", LambdaCreateTask.class);
        builder.withTask("lambdaUpdateCodeTask", LambdaUpdateCodeTask.class);
        builder.withTask("lambdaVerificationTask", LambdaVerificationTask.class);
        builder.withTask("lambdaCacheRefreshTask", LambdaCacheRefreshTask.class);
        builder.withTask("LambdaWaitToStabilizeTask", LambdaWaitToStabilizeTask.class);
        builder.withTask("lambdaVerificationTask", LambdaVerificationTask.class);
        builder.withTask("lambdaUpdateConfigTask", LambdaUpdateConfigurationTask.class);
        builder.withTask("lambdaPutConcurrencyTask", LambdaPutConcurrencyTask.class);
        builder.withTask("lambdaDeleteConcurrencyTask", LambdaDeleteConcurrencyTask.class);
        builder.withTask("lambdaEventConfigurationTask", LambdaUpdateEventConfigurationTask.class);
        builder.withTask("lambdaVerificationTask", LambdaVerificationTask.class);
        builder.withTask("lambdaUpdateAliasesTask", LambdaUpdateAliasesTask.class);
        builder.withTask("lambdaVerificationTask", LambdaVerificationTask.class);
        //has to happen so we see that there's a new version needing published.... otherwise publish will get from the cache
        // and realize it's the same version that it got from the cache in the create task, and not think it needs to
        // publish a new version.  Where-as if it's an existing lambda, it may very well BE the first version...
        builder.withTask("lambdaCacheRefreshTask", LambdaCacheRefreshTask.class);
        builder.withTask("lambdaPublishVersionTask", LambdaPublishVersionTask.class);
        builder.withTask("lambdaVerificationTask", LambdaVerificationTask.class);
        builder.withTask("lambdaCacheRefreshTask", LambdaCacheRefreshTask.class);
        builder.withTask("lambdaWaitForCachePublishTask", LambdaWaitForCachePublishTask.class);
        builder.withTask("lambdaOutputTask", LambdaOutputTask.class);
    }
}

/*
/* Copyright 2018-2025 contributors to the OpenLineage project
/* SPDX-License-Identifier: Apache-2.0
*/

package io.openlineage.spark3.agent.lifecycle.plan;

import io.openlineage.client.OpenLineage.InputDataset;
import io.openlineage.client.dataset.DatasetCompositeFacetsBuilder;
import io.openlineage.spark.agent.util.DatasetVersionUtils;
import io.openlineage.spark.api.AbstractQueryPlanInputDatasetBuilder;
import io.openlineage.spark.api.DatasetFactory;
import io.openlineage.spark.api.OpenLineageContext;
import io.openlineage.spark3.agent.utils.DataSourceV2RelationDatasetExtractor;
import io.openlineage.spark3.agent.utils.DatasetVersionDatasetFacetUtils;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.scheduler.SparkListenerEvent;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2Relation;
import org.apache.spark.sql.execution.datasources.v2.DataSourceV2ScanRelation;
import org.apache.spark.sql.execution.ui.SparkListenerSQLExecutionStart;

@Slf4j
public final class DataSourceV2ScanRelationOnStartInputDatasetBuilder
    extends AbstractQueryPlanInputDatasetBuilder<DataSourceV2ScanRelation> {
  private final DatasetFactory<InputDataset> factory;

  public DataSourceV2ScanRelationOnStartInputDatasetBuilder(
      OpenLineageContext context, DatasetFactory<InputDataset> factory) {
    super(context, true);
    this.factory = Objects.requireNonNull(factory, "parameter: factory");
  }

  @Override
  public boolean isDefinedAt(SparkListenerEvent event) {
    return event instanceof SparkListenerSQLExecutionStart;
  }

  @Override
  public boolean isDefinedAtLogicalPlan(LogicalPlan plan) {
    return plan instanceof DataSourceV2ScanRelation;
  }

  @Override
  public List<InputDataset> apply(DataSourceV2ScanRelation plan) {
    if (context.getSparkExtensionVisitorWrapper().isDefinedAt(plan.relation().table())) {
      List<InputDataset> tableInputs = getTableInputs(plan);
      return tableInputs;
    }

    DataSourceV2Relation relation = plan.relation();
    DatasetCompositeFacetsBuilder datasetFacetsBuilder = factory.createCompositeFacetBuilder();

    DatasetVersionDatasetFacetUtils.extractVersionFromDataSourceV2Relation(context, relation)
        .ifPresent(s -> DatasetVersionUtils.buildVersionFacets(context, datasetFacetsBuilder, s));
    return DataSourceV2RelationDatasetExtractor.extract(
        factory, context, relation, datasetFacetsBuilder);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}

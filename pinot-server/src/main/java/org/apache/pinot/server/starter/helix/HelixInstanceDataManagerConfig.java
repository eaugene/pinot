/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.server.starter.helix;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.pinot.common.utils.TarGzCompressionUtils;
import org.apache.pinot.segment.spi.loader.SegmentDirectoryLoaderRegistry;
import org.apache.pinot.spi.config.instance.InstanceDataManagerConfig;
import org.apache.pinot.spi.env.PinotConfiguration;
import org.apache.pinot.spi.utils.CommonConstants.Server;
import org.apache.pinot.spi.utils.ReadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.pinot.spi.utils.CommonConstants.Server.*;


/**
 * The config used for HelixInstanceDataManager.
 */
public class HelixInstanceDataManagerConfig implements InstanceDataManagerConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(HelixInstanceDataManagerConfig.class);

  // Average number of values in multi-valued columns in any table in this instance.
  // This value is used to allocate initial memory for multi-valued columns in realtime segments in consuming state.
  private static final String AVERAGE_MV_COUNT = "realtime.averageMultiValueEntriesPerRow";
  // Key of instance id
  public static final String INSTANCE_ID = "id";
  // Key of instance data directory
  public static final String INSTANCE_DATA_DIR = "dataDir";
  // Key of consumer directory
  public static final String CONSUMER_DIR = "consumerDir";
  // Key of instance segment tar directory
  public static final String INSTANCE_SEGMENT_TAR_DIR = "segmentTarDir";
  // Key of segment directory
  public static final String INSTANCE_BOOTSTRAP_SEGMENT_DIR = "bootstrap.segment.dir";
  // Key of instance level segment read mode
  public static final String READ_MODE = "readMode";
  // Key of the segment format this server can read
  public static final String SEGMENT_FORMAT_VERSION = "segment.format.version";
  // Key of whether to enable reloading consuming segments
  public static final String INSTANCE_RELOAD_CONSUMING_SEGMENT = "reload.consumingSegment";
  // Key of segment directory loader
  public static final String SEGMENT_DIRECTORY_LOADER = "segment.directory.loader";
  // Prefix for upsert config
  public static final String UPSERT_CONFIG_PREFIX = "upsert";
  // Prefix for auth config
  public static final String AUTH_CONFIG_PREFIX = "auth";
  // Prefix for tier configs
  public static final String TIER_CONFIGS_PREFIX = "tierConfigs";
  // Key of tier names
  public static final String TIER_NAMES = "tierNames";

  // Key of how many parallel realtime segments can be built.
  // A value of <= 0 indicates unlimited.
  // Unlimited parallel builds can cause high GC pauses during segment builds, causing
  // response times to suffer.
  private static final String MAX_PARALLEL_SEGMENT_BUILDS = "realtime.max.parallel.segment.builds";
  private static final int DEFAULT_MAX_PARALLEL_SEGMENT_BUILDS = 4;

  // Key of how many parallel segment downloads can be made per table.
  // A value of <= 0 indicates unlimited.
  // Unlimited parallel downloads can make Pinot controllers receive high burst of download requests,
  // causing controllers unavailable for that period of time.
  private static final String MAX_PARALLEL_SEGMENT_DOWNLOADS = "table.level.max.parallel.segment.downloads";
  private static final int DEFAULT_MAX_PARALLEL_SEGMENT_DOWNLOADS = -1;

  // Key of server segment download rate limit
  // limit the rate to write download-untar stream to disk, in bytes
  // -1 for no disk write limit, 0 for limit the writing to min(untar, download) rate
  private static final String STREAM_SEGMENT_DOWNLOAD_UNTAR_RATE_LIMIT =
      "segment.stream.download.untar.rate.limit.bytes.per.sec";
  private static final long DEFAULT_STREAM_SEGMENT_DOWNLOAD_UNTAR_RATE_LIMIT =
      TarGzCompressionUtils.NO_DISK_WRITE_RATE_LIMIT;

  // Key of whether to use streamed server segment download-untar
  private static final String ENABLE_STREAM_SEGMENT_DOWNLOAD_UNTAR = "segment.stream.download.untar";
  private static final boolean DEFAULT_ENABLE_STREAM_SEGMENT_DOWNLOAD_UNTAR = false;

  // Whether memory for realtime consuming segments should be allocated off-heap.
  private static final String REALTIME_OFFHEAP_ALLOCATION = "realtime.alloc.offheap";
  // And whether the allocation should be direct (default is to allocate via mmap)
  // Direct memory allocation may mean setting heap size appropriately when starting JVM.
  // The metric ServerGauge.REALTIME_OFFHEAP_MEMORY_USED should indicate how much memory is needed.
  private static final String DIRECT_REALTIME_OFFHEAP_ALLOCATION = "realtime.alloc.offheap.direct";

  // Number of simultaneous segments that can be refreshed on one server.
  // Segment refresh works by loading the old as well as new versions of segments in memory, assigning
  // new incoming queries to use the new version. The old version is dropped when all the queries that
  // use the old version have completed. A server-wide semaphore is acquired before refreshing a segment so
  // that we exceed the memory in some limited fashion. If there are multiple
  // refresh requests, then they are queued on the semaphore (FIFO).
  // In some multi-tenant use cases, it may be fine to over-allocate memory.
  // Setting this config variable to a value greater than 1 will cause as many refresh threads to run simultaneously.
  //
  // NOTE: While segment load can be faster, multiple threads will be taken up loading segments, so
  //       it is possible that the query latencies increase during that period.
  //
  private static final String MAX_PARALLEL_REFRESH_THREADS = "max.parallel.refresh.threads";

  // To preload segments of table using upsert in parallel for fast upsert metadata recovery.
  private static final String MAX_SEGMENT_PRELOAD_THREADS = "max.segment.preload.threads";

  // Size of cache that holds errors.
  private static final String ERROR_CACHE_SIZE = "error.cache.size";

  private static final String DELETED_SEGMENTS_CACHE_SIZE = "table.deleted.segments.cache.size";
  private static final String DELETED_SEGMENTS_CACHE_TTL_MINUTES = "table.deleted.segments.cache.ttl.minutes";
  private static final String PEER_DOWNLOAD_SCHEME = "peer.download.scheme";

  // Check if the external view is dropped for a table, and if so, wait for the external view to
  // be updated for a maximum of this time.
  private static final String EXTERNAL_VIEW_DROPPED_MAX_WAIT_MS = "external.view.dropped.max.wait.ms";
  private static final String EXTERNAL_VIEW_DROPPED_CHECK_INTERVAL_MS = "external.view.dropped.check.interval.ms";

  private final static String[] REQUIRED_KEYS = {INSTANCE_ID};
  private static final long DEFAULT_ERROR_CACHE_SIZE = 100L;
  private static final int DEFAULT_DELETED_SEGMENTS_CACHE_SIZE = 10_000;
  private static final int DEFAULT_DELETED_SEGMENTS_CACHE_TTL_MINUTES = 2;
  public static final long DEFAULT_EXTERNAL_VIEW_DROPPED_MAX_WAIT_MS = 20 * 60_000L;
  public static final long DEFAULT_EXTERNAL_VIEW_DROPPED_CHECK_INTERVAL_MS = 1_000L;

  private final PinotConfiguration _serverConfig;
  private final PinotConfiguration _upsertConfig;
  private final PinotConfiguration _authConfig;
  private final Map<String, Map<String, String>> _tierConfigs;

  public HelixInstanceDataManagerConfig(PinotConfiguration serverConfig)
      throws ConfigurationException {
    _serverConfig = serverConfig;

    for (String key : serverConfig.getKeys()) {
      LOGGER.info("InstanceDataManagerConfig, key: {} , value: {}", key, serverConfig.getProperty(key));
    }

    checkRequiredKeys();

    _authConfig = serverConfig.subset(AUTH_CONFIG_PREFIX);
    _upsertConfig = serverConfig.subset(UPSERT_CONFIG_PREFIX);

    PinotConfiguration tierConfigs = getConfig().subset(TIER_CONFIGS_PREFIX);
    List<String> tierNames = tierConfigs.getProperty(TIER_NAMES, Collections.emptyList());
    if (tierNames.isEmpty()) {
      _tierConfigs = Collections.emptyMap();
    } else {
      _tierConfigs = Maps.newHashMapWithExpectedSize(tierNames.size());
      for (String tierName : tierNames) {
        Map<String, String> tierConfigMap = new HashMap<>();
        tierConfigs.subset(tierName).toMap().forEach((k, v) -> tierConfigMap.put(k, String.valueOf(v)));
        _tierConfigs.put(tierName, tierConfigMap);
      }
    }
  }

  private void checkRequiredKeys()
      throws ConfigurationException {
    for (String keyString : REQUIRED_KEYS) {
      Optional.ofNullable(_serverConfig.getProperty(keyString))
          .orElseThrow(() -> new ConfigurationException("Cannot find required key : " + keyString));
    }
  }

  @Override
  public PinotConfiguration getConfig() {
    return _serverConfig;
  }

  @Override
  public String getInstanceId() {
    return _serverConfig.getProperty(INSTANCE_ID);
  }

  @Override
  public String getInstanceDataDir() {
    return _serverConfig.getProperty(INSTANCE_DATA_DIR, DEFAULT_INSTANCE_DATA_DIR);
  }

  @Override
  public String getConsumerDir() {
    return _serverConfig.getProperty(CONSUMER_DIR);
  }

  @Override
  public String getInstanceSegmentTarDir() {
    return _serverConfig.getProperty(INSTANCE_SEGMENT_TAR_DIR, DEFAULT_INSTANCE_SEGMENT_TAR_DIR);
  }

  @Override
  public String getRealtimeSegmentConsumerClientIdSuffix() {
    return _serverConfig.getProperty(CONFIG_OF_REALTIME_SEGMENT_CONSUMER_CLIENT_ID_SUFFIX,
        DEFAULT_REALTIME_SEGMENT_CONSUMER_CLIENT_ID_SUFFIX);
  }

  @Override
  public String getInstanceBootstrapSegmentDir() {
    return _serverConfig.getProperty(INSTANCE_BOOTSTRAP_SEGMENT_DIR);
  }

  @Override
  public String getSegmentStoreUri() {
    return _serverConfig.getProperty(CONFIG_OF_SEGMENT_STORE_URI);
  }

  @Override
  public ReadMode getReadMode() {
    return ReadMode.valueOf(_serverConfig.getProperty(READ_MODE, DEFAULT_READ_MODE));
  }

  @Override
  public String getSegmentFormatVersion() {
    return _serverConfig.getProperty(SEGMENT_FORMAT_VERSION);
  }

  @Override
  public boolean isRealtimeOffHeapAllocation() {
    return _serverConfig.getProperty(REALTIME_OFFHEAP_ALLOCATION, true);
  }

  @Override
  public boolean isDirectRealtimeOffHeapAllocation() {
    return _serverConfig.getProperty(DIRECT_REALTIME_OFFHEAP_ALLOCATION, false);
  }

  public boolean shouldReloadConsumingSegment() {
    return _serverConfig.getProperty(INSTANCE_RELOAD_CONSUMING_SEGMENT, Server.DEFAULT_RELOAD_CONSUMING_SEGMENT);
  }

  @Override
  public String getAvgMultiValueCount() {
    return _serverConfig.getProperty(AVERAGE_MV_COUNT);
  }

  public int getMaxParallelRefreshThreads() {
    return _serverConfig.getProperty(MAX_PARALLEL_REFRESH_THREADS, 1);
  }

  public int getMaxSegmentPreloadThreads() {
    return _serverConfig.getProperty(MAX_SEGMENT_PRELOAD_THREADS, 0);
  }

  public int getMaxParallelSegmentBuilds() {
    return _serverConfig.getProperty(MAX_PARALLEL_SEGMENT_BUILDS, DEFAULT_MAX_PARALLEL_SEGMENT_BUILDS);
  }

  @Override
  public int getMaxParallelSegmentDownloads() {
    return _serverConfig.getProperty(MAX_PARALLEL_SEGMENT_DOWNLOADS, DEFAULT_MAX_PARALLEL_SEGMENT_DOWNLOADS);
  }

  public String getSegmentDirectoryLoader() {
    return _serverConfig.getProperty(SEGMENT_DIRECTORY_LOADER,
        SegmentDirectoryLoaderRegistry.DEFAULT_SEGMENT_DIRECTORY_LOADER_NAME);
  }

  @Override
  public long getErrorCacheSize() {
    return _serverConfig.getProperty(ERROR_CACHE_SIZE, DEFAULT_ERROR_CACHE_SIZE);
  }

  @Override
  public boolean isStreamSegmentDownloadUntar() {
    return _serverConfig.getProperty(ENABLE_STREAM_SEGMENT_DOWNLOAD_UNTAR,
        DEFAULT_ENABLE_STREAM_SEGMENT_DOWNLOAD_UNTAR);
  }

  @Override
  public long getStreamSegmentDownloadUntarRateLimit() {
    return _serverConfig.getProperty(STREAM_SEGMENT_DOWNLOAD_UNTAR_RATE_LIMIT,
        DEFAULT_STREAM_SEGMENT_DOWNLOAD_UNTAR_RATE_LIMIT);
  }

  @Override
  public int getDeletedSegmentsCacheSize() {
    return _serverConfig.getProperty(DELETED_SEGMENTS_CACHE_SIZE, DEFAULT_DELETED_SEGMENTS_CACHE_SIZE);
  }

  @Override
  public int getDeletedSegmentsCacheTtlMinutes() {
    return _serverConfig.getProperty(DELETED_SEGMENTS_CACHE_TTL_MINUTES, DEFAULT_DELETED_SEGMENTS_CACHE_TTL_MINUTES);
  }

  @Override
  public String getSegmentPeerDownloadScheme() {
    return _serverConfig.getProperty(PEER_DOWNLOAD_SCHEME);
  }

  @Override
  public long getExternalViewDroppedMaxWaitMs() {
    return _serverConfig.getProperty(EXTERNAL_VIEW_DROPPED_MAX_WAIT_MS, DEFAULT_EXTERNAL_VIEW_DROPPED_MAX_WAIT_MS);
  }

  @Override
  public long getExternalViewDroppedCheckIntervalMs() {
    return _serverConfig.getProperty(EXTERNAL_VIEW_DROPPED_CHECK_INTERVAL_MS,
        DEFAULT_EXTERNAL_VIEW_DROPPED_CHECK_INTERVAL_MS);
  }

  @Override
  public PinotConfiguration getUpsertConfig() {
    return _upsertConfig;
  }

  @Override
  public PinotConfiguration getAuthConfig() {
    return _authConfig;
  }

  @Override
  public Map<String, Map<String, String>> getTierConfigs() {
    return _tierConfigs;
  }
}

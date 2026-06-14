```
#!/usr/bin/env python3
"""
GKE 资源请求与使用量历史分析脚本
通过 Google Cloud Monitoring API 获取近一个月数据
"""

import os
import argparse
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Tuple, Optional
import pandas as pd
import matplotlib.pyplot as plt
from google.cloud import monitoring_v3
from google.cloud.monitoring_v3.types import TimeSeries
import matplotlib.dates as mdates

class GkeResourceAnalyzer:
    def __init__(self, project_id: str, cluster_name: str, location: str):
        """
        初始化分析器
        
        Args:
            project_id: GCP 项目ID
            cluster_name: GKE 集群名称
            location: 集群区域，如 'us-central1'
        """
        self.project_id = project_id
        self.cluster_name = cluster_name
        self.location = location
        self.client = monitoring_v3.MetricServiceClient()
        
    def _build_filter(self, metric_type: str, resource_type: str = "k8s_container") -> str:
        """构建监控查询过滤器"""
        return (
            f'resource.type="{resource_type}" '
            f'AND resource.labels.project_id="{self.project_id}" '
            f'AND resource.labels.location="{self.location}" '
            f'AND resource.labels.cluster_name="{self.cluster_name}" '
            f'AND metric.type="{metric_type}"'
        )
    
    def query_time_series(
        self, 
        metric_type: str, 
        days: int = 30,
        aggregation: Optional[str] = "mean"
    ) -> Dict[str, List[Tuple[datetime, float]]]:
        """
        查询时间序列数据
        
        Args:
            metric_type: 指标类型，如 "kubernetes.io/container/cpu/request_cores"
            days: 查询天数
            aggregation: 聚合方法，如 "mean", "max", "min", "sum"
            
        Returns:
            字典: {pod_name: [(timestamp, value), ...]}
        """
        # 构建查询请求
        project_name = f"projects/{self.project_id}"
        
        interval = monitoring_v3.TimeInterval({
            "end_time": datetime.now(timezone.utc),
            "start_time": datetime.now(timezone.utc) - timedelta(days=days)
        })
        
        # 设置聚合
        if aggregation == "mean":
            aggregator = monitoring_v3.Aggregation({
                "alignment_period": {"seconds": 3600},  # 1小时对齐
                "per_series_aligner": monitoring_v3.Aggregation.Aligner.ALIGN_MEAN,
                "cross_series_reducer": monitoring_v3.Aggregation.Reducer.REDUCE_MEAN,
                "group_by_fields": ["resource.labels.pod_name"]
            })
        elif aggregation == "max":
            aggregator = monitoring_v3.Aggregation({
                "alignment_period": {"seconds": 3600},
                "per_series_aligner": monitoring_v3.Aggregation.Aligner.ALIGN_MAX,
                "cross_series_reducer": monitoring_v3.Aggregation.Reducer.REDUCE_MAX,
                "group_by_fields": ["resource.labels.pod_name"]
            })
        else:
            aggregator = None
        
        # 执行查询
        filter_str = self._build_filter(metric_type)
        request = monitoring_v3.ListTimeSeriesRequest(
            name=project_name,
            filter=filter_str,
            interval=interval,
            view=monitoring_v3.ListTimeSeriesRequest.TimeSeriesView.FULL,
            aggregation=aggregator if aggregator else None
        )
        
        # 处理结果
        results = {}
        try:
            for series in self.client.list_time_series(request=request):
                pod_name = series.resource.labels.get("pod_name", "unknown")
                
                # 提取时间序列数据点
                data_points = []
                for point in series.points:
                    timestamp = point.interval.end_time
                    value = point.value.double_value or point.value.int64_value or 0
                    
                    if timestamp and value is not None:
                        dt = timestamp.replace(tzinfo=None)
                        data_points.append((dt, float(value)))
                
                if data_points:
                    # 按时间排序
                    data_points.sort(key=lambda x: x[0])
                    results[pod_name] = data_points
                    
        except Exception as e:
            print(f"查询指标 {metric_type} 时出错: {e}")
            
        return results
    
    def get_cpu_request_usage(self, days: int = 30) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        获取CPU请求和使用量数据
        
        Returns:
            (request_df, usage_df): 两个DataFrame，索引为时间，列为pod
        """
        print("正在查询CPU请求数据...")
        cpu_request = self.query_time_series(
            "kubernetes.io/container/cpu/request_cores", 
            days=days,
            aggregation="mean"
        )
        
        print("正在查询CPU使用量数据...")
        cpu_usage = self.query_time_series(
            "kubernetes.io/container/cpu/core_usage_time", 
            days=days,
            aggregation="mean"
        )
        
        # 转换为DataFrame
        request_df = self._to_dataframe(cpu_request, "cpu_request_cores")
        usage_df = self._to_dataframe(cpu_usage, "cpu_usage_cores")
        
        return request_df, usage_df
    
    def get_memory_request_usage(self, days: int = 30) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        获取内存请求和使用量数据（单位：bytes）
        
        Returns:
            (request_df, usage_df): 两个DataFrame，索引为时间，列为pod
        """
        print("正在查询内存请求数据...")
        mem_request = self.query_time_series(
            "kubernetes.io/container/memory/request_bytes", 
            days=days,
            aggregation="mean"
        )
        
        print("正在查询内存使用量数据...")
        mem_usage = self.query_time_series(
            "kubernetes.io/container/memory/used_bytes", 
            days=days,
            aggregation="mean"
        )
        
        # 转换为DataFrame
        request_df = self._to_dataframe(mem_request, "mem_request_bytes")
        usage_df = self._to_dataframe(mem_usage, "mem_usage_bytes")
        
        return request_df, usage_df
    
    def _to_dataframe(self, data: Dict[str, List[Tuple[datetime, float]]], 
                     value_name: str) -> pd.DataFrame:
        """将时间序列数据转换为DataFrame"""
        if not data:
            return pd.DataFrame()
        
        # 创建多级索引的Series列表
        series_list = []
        for pod_name, points in data.items():
            if points:
                timestamps, values = zip(*points)
                series = pd.Series(
                    values, 
                    index=timestamps, 
                    name=pod_name
                )
                series_list.append(series)
        
        if not series_list:
            return pd.DataFrame()
        
        # 合并为DataFrame
        df = pd.concat(series_list, axis=1)
        df.index.name = "timestamp"
        
        # 重采样到每日频率，填充缺失值
        df = df.resample("D").mean()
        
        return df
    
    def analyze_waste(self, request_df: pd.DataFrame, usage_df: pd.DataFrame, 
                     resource_type: str = "cpu") -> pd.DataFrame:
        """
        分析资源浪费情况
        
        Args:
            request_df: 请求量DataFrame
            usage_df: 使用量DataFrame
            resource_type: 资源类型，'cpu' 或 'memory'
            
        Returns:
            包含浪费分析结果的DataFrame
        """
        # 对齐两个DataFrame
        common_pods = set(request_df.columns) & set(usage_df.columns)
        common_pods = sorted(common_pods)
        
        if not common_pods:
            print("警告: 没有找到共同的Pod数据")
            return pd.DataFrame()
        
        # 计算平均请求和使用量
        results = []
        for pod in common_pods:
            avg_request = request_df[pod].mean()
            avg_usage = usage_df[pod].mean()
            
            if avg_request > 0:
                usage_ratio = avg_usage / avg_request if avg_request > 0 else 0
                waste_ratio = 1 - usage_ratio if usage_ratio <= 1 else 0
                
                results.append({
                    "pod": pod,
                    f"avg_request_{resource_type}": avg_request,
                    f"avg_usage_{resource_type}": avg_usage,
                    f"usage_ratio_{resource_type}": usage_ratio,
                    f"waste_ratio_{resource_type}": waste_ratio,
                    "status": "过度分配" if usage_ratio < 0.3 else "合理" if usage_ratio < 0.7 else "紧张"
                })
        
        return pd.DataFrame(results)
    
    def generate_report(self, days: int = 30, output_dir: str = "./gke_report"):
        """
        生成完整的资源分析报告
        
        Args:
            days: 分析天数
            output_dir: 输出目录
        """
        os.makedirs(output_dir, exist_ok=True)
        
        print("=" * 60)
        print(f"GKE 资源使用分析报告")
        print(f"项目: {self.project_id}")
        print(f"集群: {self.cluster_name} ({self.location})")
        print(f"分析周期: 最近{days}天")
        print("=" * 60)
        
        # 获取数据
        cpu_request_df, cpu_usage_df = self.get_cpu_request_usage(days)
        mem_request_df, mem_usage_df = self.get_memory_request_usage(days)
        
        # 分析浪费情况
        cpu_waste_df = self.analyze_waste(cpu_request_df, cpu_usage_df, "cpu")
        mem_waste_df = self.analyze_waste(mem_request_df, mem_usage_df, "memory")
        
        # 生成报告
        self._save_dataframes(cpu_request_df, cpu_usage_df, mem_request_df, 
                             mem_usage_df, cpu_waste_df, mem_waste_df, output_dir)
        
        self._create_visualizations(cpu_request_df, cpu_usage_df, 
                                   mem_request_df, mem_usage_df, output_dir)
        
        self._print_summary(cpu_waste_df, mem_waste_df)
        
        print(f"\n报告已生成到目录: {output_dir}")
    
    def _save_dataframes(self, cpu_req, cpu_use, mem_req, mem_use, 
                        cpu_waste, mem_waste, output_dir):
        """保存DataFrame到CSV文件"""
        cpu_req.to_csv(f"{output_dir}/cpu_request.csv")
        cpu_use.to_csv(f"{output_dir}/cpu_usage.csv")
        mem_req.to_csv(f"{output_dir}/memory_request.csv")
        mem_use.to_csv(f"{output_dir}/memory_usage.csv")
        cpu_waste.to_csv(f"{output_dir}/cpu_waste_analysis.csv")
        mem_waste.to_csv(f"{output_dir}/memory_waste_analysis.csv")
        
        print(f"\n数据已保存到CSV文件:")
        print(f"  - CPU请求量: {output_dir}/cpu_request.csv")
        print(f"  - CPU使用量: {output_dir}/cpu_usage.csv")
        print(f"  - 内存请求量: {output_dir}/memory_request.csv")
        print(f"  - 内存使用量: {output_dir}/memory_usage.csv")
        print(f"  - CPU浪费分析: {output_dir}/cpu_waste_analysis.csv")
        print(f"  - 内存浪费分析: {output_dir}/memory_waste_analysis.csv")
    
    def _create_visualizations(self, cpu_req, cpu_use, mem_req, mem_use, output_dir):
        """创建可视化图表"""
        plt.style.use('seaborn-v0_8-darkgrid')
        fig, axes = plt.subplots(2, 2, figsize=(16, 10))
        
        # 选择前5个Pod进行可视化
        pods_to_plot = min(5, len(cpu_req.columns))
        top_pods = cpu_req.mean().nlargest(pods_to_plot).index
        
        # 1. CPU请求 vs 使用量
        if not cpu_req.empty and not cpu_use.empty:
            for pod in top_pods:
                if pod in cpu_req.columns and pod in cpu_use.columns:
                    axes[0, 0].plot(cpu_req.index, cpu_req[pod], '--', label=f'{pod} (请求)', alpha=0.7)
                    axes[0, 0].plot(cpu_use.index, cpu_use[pod], '-', label=f'{pod} (使用)', alpha=0.7)
            
            axes[0, 0].set_title('CPU核心数: 请求 vs 使用量')
            axes[0, 0].set_xlabel('日期')
            axes[0, 0].set_ylabel('CPU核心数')
            axes[0, 0].legend(bbox_to_anchor=(1.05, 1), loc='upper left')
            axes[0, 0].xaxis.set_major_formatter(mdates.DateFormatter('%m-%d'))
            axes[0, 0].tick_params(axis='x', rotation=45)
        
        # 2. 内存请求 vs 使用量 (转换为GiB)
        if not mem_req.empty and not mem_use.empty:
            for pod in top_pods:
                if pod in mem_req.columns and pod in mem_use.columns:
                    axes[0, 1].plot(mem_req.index, mem_req[pod] / 1024**3, '--', 
                                   label=f'{pod} (请求)', alpha=0.7)
                    axes[0, 1].plot(mem_use.index, mem_use[pod] / 1024**3, '-', 
                                   label=f'{pod} (使用)', alpha=0.7)
            
            axes[0, 1].set_title('内存: 请求 vs 使用量')
            axes[0, 1].set_xlabel('日期')
            axes[0, 1].set_ylabel('内存 (GiB)')
            axes[0, 1].legend(bbox_to_anchor=(1.05, 1), loc='upper left')
            axes[0, 1].xaxis.set_major_formatter(mdates.DateFormatter('%m-%d'))
            axes[0, 1].tick_params(axis='x', rotation=45)
        
        # 3. CPU使用率热图
        if not cpu_req.empty and not cpu_use.empty:
            usage_ratio = pd.DataFrame()
            for pod in top_pods:
                if pod in cpu_req.columns and pod in cpu_use.columns:
                    usage_ratio[pod] = cpu_use[pod] / cpu_req[pod].clip(lower=0.001)
            
            if not usage_ratio.empty:
                im = axes[1, 0].imshow(usage_ratio.T, aspect='auto', cmap='RdYlGn', 
                                      vmin=0, vmax=1)
                axes[1, 0].set_title('CPU使用率 (使用量/请求量)')
                axes[1, 0].set_xlabel('时间点')
                axes[1, 0].set_ylabel('Pod')
                axes[1, 0].set_yticks(range(len(top_pods)))
                axes[1, 0].set_yticklabels(top_pods)
                plt.colorbar(im, ax=axes[1, 0], label='使用率')
        
        # 4. 浪费最严重的Pod
        if not cpu_req.empty and not cpu_use.empty:
            waste_scores = {}
            for pod in cpu_req.columns:
                if pod in cpu_use.columns:
                    avg_req = cpu_req[pod].mean()
                    avg_use = cpu_use[pod].mean()
                    if avg_req > 0:
                        waste = 1 - (avg_use / avg_req)
                        if waste > 0:
                            waste_scores[pod] = waste
            
            if waste_scores:
                top_waste = sorted(waste_scores.items(), key=lambda x: x[1], reverse=True)[:10]
                pods, wastes = zip(*top_waste) if top_waste else ([], [])
                
                bars = axes[1, 1].barh(range(len(pods)), wastes, color='skyblue')
                axes[1, 1].set_yticks(range(len(pods)))
                axes[1, 1].set_yticklabels(pods)
                axes[1, 1].set_xlabel('浪费比例 (1 - 使用率)')
                axes[1, 1].set_title('CPU资源浪费Top 10')
                
                # 添加数值标签
                for bar, waste in zip(bars, wastes):
                    width = bar.get_width()
                    axes[1, 1].text(width + 0.01, bar.get_y() + bar.get_height()/2,
                                  f'{waste:.1%}', ha='left', va='center')
        
        plt.tight_layout()
        plt.savefig(f'{output_dir}/resource_analysis.png', dpi=150, bbox_inches='tight')
        plt.close()
        
        print(f"图表已保存: {output_dir}/resource_analysis.png")
    
    def _print_summary(self, cpu_waste_df, mem_waste_df):
        """打印分析摘要"""
        print("\n" + "=" * 60)
        print("资源浪费分析摘要")
        print("=" * 60)
        
        if not cpu_waste_df.empty:
            print("\n🔍 CPU资源分析:")
            print(f"  分析Pod数量: {len(cpu_waste_df)}")
            
            over_allocated = cpu_waste_df[cpu_waste_df['status'] == '过度分配']
            if not over_allocated.empty:
                print(f"  ⚠️  过度分配的Pod: {len(over_allocated)}个")
                print("    浪费最严重的Pod:")
                for _, row in over_allocated.nlargest(5, 'waste_ratio_cpu').iterrows():
                    print(f"      - {row['pod']}: 请求{row['avg_request_cpu']:.2f}核, "
                          f"使用{row['avg_usage_cpu']:.2f}核, "
                          f"浪费{row['waste_ratio_cpu']:.1%}")
            
            total_request = cpu_waste_df['avg_request_cpu'].sum()
            total_usage = cpu_waste_df['avg_usage_cpu'].sum()
            if total_request > 0:
                overall_utilization = total_usage / total_request
                print(f"  📊 总体CPU利用率: {overall_utilization:.1%}")
        
        if not mem_waste_df.empty:
            print("\n💾 内存资源分析:")
            print(f"  分析Pod数量: {len(mem_waste_df)}")
            
            over_allocated = mem_waste_df[mem_waste_df['status'] == '过度分配']
            if not over_allocated.empty:
                print(f"  ⚠️  过度分配的Pod: {len(over_allocated)}个")
                print("    浪费最严重的Pod:")
                for _, row in over_allocated.nlargest(3, 'waste_ratio_memory').iterrows():
                    print(f"      - {row['pod']}: 请求{row['avg_request_memory']/1024**3:.1f}GiB, "
                          f"使用{row['avg_usage_memory']/1024**3:.1f}GiB, "
                          f"浪费{row['waste_ratio_memory']:.1%}")


def main():
    parser = argparse.ArgumentParser(description='分析GKE集群资源请求与使用量')
    parser.add_argument('--project-id', required=True, help='GCP项目ID')
    parser.add_argument('--cluster-name', required=True, help='GKE集群名称')
    parser.add_argument('--location', required=True, help='集群区域，如 us-central1')
    parser.add_argument('--days', type=int, default=30, help='分析天数，默认30天')
    parser.add_argument('--output-dir', default='./gke_report', help='输出目录')
    
    args = parser.parse_args()
    
    # 设置认证
    os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = input(
        "请输入服务账号密钥文件路径 (留空使用默认认证): "
    ).strip() or os.environ.get('GOOGLE_APPLICATION_CREDENTIALS', '')
    
    if not os.environ.get('GOOGLE_APPLICATION_CREDENTIALS'):
        print("警告: 未设置GOOGLE_APPLICATION_CREDENTIALS环境变量")
        print("将尝试使用gcloud默认认证...")
    
    # 创建分析器并生成报告
    analyzer = GkeResourceAnalyzer(args.project_id, args.cluster_name, args.location)
    analyzer.generate_report(days=args.days, output_dir=args.output_dir)


if __name__ == "__main__":
    main()

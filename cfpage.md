'''python
#!/usr/bin/env python3
from mcp.server.fastmcp import FastMCP
from google.cloud import compute_v1
from google.oauth2 import service_account
from datetime import datetime, timezone
import os

mcp = FastMCP("Google Cloud Compute MCP")

def get_gcp_client():
    """获取 GCP 客户端，支持环境变量或默认凭据"""
    credentials_path = os.getenv('GOOGLE_APPLICATION_CREDENTIALS')
    if credentials_path and os.path.exists(credentials_path):
        credentials = service_account.Credentials.from_service_account_file(credentials_path)
        return compute_v1.ImagesClient(credentials=credentials), compute_v1.InstancesClient(credentials=credentials)
    return compute_v1.ImagesClient(), compute_v1.InstancesClient()

@mcp.tool()
async def list_vm_instances(project_id: str, zone: str) -> str:
    """列出指定区域的所有 VM 实例及其详细信息"""
    try:
        _, instance_client = get_gcp_client()
        request = compute_v1.ListInstancesRequest(project=project_id, zone=zone)
        instances = instance_client.list(request)
        
        result = []
        for instance in instances:
            # 获取启动磁盘镜像信息
            boot_disk = instance.disks[0] if instance.disks else None
            image_info = boot_disk.source if boot_disk else "N/A"
            
            result.append({
                "name": instance.name,
                "status": instance.status,
                "machine_type": instance.machine_type.split('/')[-1],
                "internal_ip": instance.network_interfaces[0].network_ip if instance.network_interfaces else "N/A",
                "external_ip": instance.network_interfaces[0].access_configs[0].nat_i_p if instance.network_interfaces and instance.network_interfaces[0].access_configs else "N/A",
                "boot_image": image_info,
                "creation_time": instance.creation_timestamp
            })
        
        return f"Found {len(result)} instances in zone {zone}:\n" + "\n".join([str(vm) for vm in result])
    except Exception as e:
        return f"Error listing instances: {str(e)}"

@mcp.tool()
async def get_vm_details(project_id: str, zone: str, instance_name: str) -> str:
    """获取特定 VM 实例的详细信息"""
    try:
        _, instance_client = get_gcp_client()
        request = compute_v1.GetInstanceRequest(project=project_id, zone=zone, instance=instance_name)
        instance = instance_client.get(request)
        
        details = {
            "name": instance.name,
            "status": instance.status,
            "machine_type": instance.machine_type,
            "cpu_platform": instance.cpu_platform,
            "creation_time": instance.creation_timestamp,
            "labels": instance.labels if instance.labels else {}
        }
        
        # 网络信息
        if instance.network_interfaces:
            network = instance.network_interfaces[0]
            details["network"] = {
                "network": network.network,
                "subnetwork": network.subnetwork,
                "internal_ip": network.network_ip,
                "external_ip": network.access_configs[0].nat_i_p if network.access_configs else None
            }
        
        return str(details)
    except Exception as e:
        return f"Error getting VM details: {str(e)}"

@mcp.tool()
async def list_images(project_id: str) -> str:
    """列出项目中的所有镜像"""
    try:
        image_client, _ = get_gcp_client()
        request = compute_v1.ListImagesRequest(project=project_id)
        images = image_client.list(request)
        
        result = []
        for image in images:
            result.append({
                "name": image.name,
                "family": image.family if image.family else "N/A",
                "status": image.status,
                "disk_size_gb": image.disk_size_gb,
                "creation_timestamp": image.creation_timestamp
            })
        
        return f"Found {len(result)} images in project {project_id}:\n" + "\n".join([str(img) for img in result])
    except Exception as e:
        return f"Error listing images: {str(e)}"

@mcp.tool()
async def check_image_age_warning(project_id: str, max_age_days: int = 90) -> str:
    """检查镜像是否超过指定天数，发出警告"""
    try:
        image_client, _ = get_gcp_client()
        request = compute_v1.ListImagesRequest(project=project_id)
        images = image_client.list(request)
        
        now = datetime.now(timezone.utc)
        warnings = []
        
        for image in images:
            if image.creation_timestamp:
                # 解析时间戳
                created_time = datetime.fromisoformat(image.creation_timestamp.rstrip('Z')).replace(tzinfo=timezone.utc)
                age_days = (now - created_time).days
                
                if age_days > max_age_days:
                    warnings.append(f"⚠️ WARNING: Image '{image.name}' is {age_days} days old (threshold: {max_age_days} days)")
        
        if warnings:
            return "\n".join(warnings)
        else:
            return f"✅ All images in project {project_id} are within the {max_age_days}-day age threshold."
    except Exception as e:
        return f"Error checking image age: {str(e)}"

if __name__ == "__main__":
    mcp.run()
'''
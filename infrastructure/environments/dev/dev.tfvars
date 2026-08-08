resource_group_name  = "rg-reliability-platform-dev"
virtual_network_name = "vnet-envforge-dev"
location             = "italynorth"

virtual_network_address_space = [
  "10.20.0.0/16"
]

aks_subnet_address_prefixes = [
  "10.20.0.0/22"
]

private_endpoints_subnet_address_prefixes = [
  "10.20.4.0/24"
]

tags = {
  application = "envforge"
  environment = "dev"
  managed-by  = "terraform"
  project     = "reliability-platform"
}
{{- define "envforge-workload.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "envforge-workload.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "envforge-workload.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{- define "envforge-workload.labels" -}}
app.kubernetes.io/name: {{ include "envforge-workload.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
envforge.io/managed: "true"
envforge.io/environment: {{ .Values.environment.name | quote }}
envforge.io/template: {{ .Values.environment.template | quote }}
envforge.io/owner: {{ .Values.environment.owner | quote }}
{{- end }}
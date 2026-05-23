{{- define "movienight.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "movienight.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := include "movienight.name" . -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "movienight.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" -}}
{{- end -}}

{{- define "movienight.labels" -}}
helm.sh/chart: {{ include "movienight.chart" . }}
{{ include "movienight.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.global.labels }}
{{ toYaml . }}
{{- end }}
{{- end -}}

{{- define "movienight.selectorLabels" -}}
app.kubernetes.io/name: {{ include "movienight.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "movienight.componentLabels" -}}
{{- $root := .root -}}
{{- $component := .component -}}
{{ include "movienight.labels" $root }}
app.kubernetes.io/component: {{ $component }}
{{- end -}}

{{- define "movienight.componentSelectorLabels" -}}
{{- $root := .root -}}
{{- $component := .component -}}
{{ include "movienight.selectorLabels" $root }}
app.kubernetes.io/component: {{ $component }}
{{- end -}}

{{- define "movienight.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "movienight.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{- define "movienight.gatewayName" -}}
{{- if .Values.gateway.name -}}
{{- .Values.gateway.name -}}
{{- else -}}
{{- printf "%s-gateway" (include "movienight.fullname" .) -}}
{{- end -}}
{{- end -}}

{{- define "movienight.postgresClusterName" -}}
{{- if .Values.postgres.cluster.name -}}
{{- .Values.postgres.cluster.name -}}
{{- else -}}
{{- printf "%s-postgres" (include "movienight.fullname" .) -}}
{{- end -}}
{{- end -}}

{{- define "movienight.postgresHost" -}}
{{- default (printf "%s-rw" (include "movienight.postgresClusterName" .)) .Values.postgres.cluster.host -}}
{{- end -}}

{{- define "movienight.postgresJdbcUrl" -}}
{{- printf "jdbc:postgresql://%s:%v/%s" (include "movienight.postgresHost" .) (default 5432 .Values.postgres.cluster.port) .Values.postgres.cluster.database -}}
{{- end -}}

{{- define "movienight.postgresEnv" -}}
{{- if .Values.postgres.url }}
- name: SPRING_DATASOURCE_URL
  value: {{ .Values.postgres.url | quote }}
{{- if .Values.postgres.username }}
- name: SPRING_DATASOURCE_USERNAME
  value: {{ .Values.postgres.username | quote }}
{{- end }}
{{- if .Values.postgres.password }}
- name: SPRING_DATASOURCE_PASSWORD
  value: {{ .Values.postgres.password | quote }}
{{- end }}
{{- else if .Values.postgres.existingSecret.name }}
- name: SPRING_DATASOURCE_URL
  valueFrom:
    secretKeyRef:
      name: {{ .Values.postgres.existingSecret.name }}
      key: {{ .Values.postgres.existingSecret.urlKey }}
- name: SPRING_DATASOURCE_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Values.postgres.existingSecret.name }}
      key: {{ .Values.postgres.existingSecret.usernameKey }}
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.postgres.existingSecret.name }}
      key: {{ .Values.postgres.existingSecret.passwordKey }}
{{- else if .Values.postgres.cluster.enabled }}
- name: SPRING_DATASOURCE_URL
  value: {{ include "movienight.postgresJdbcUrl" . | quote }}
- name: SPRING_DATASOURCE_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ required "postgres.cluster.bootstrapSecretName is required when postgres.cluster.enabled=true" .Values.postgres.cluster.bootstrapSecretName }}
      key: username
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ required "postgres.cluster.bootstrapSecretName is required when postgres.cluster.enabled=true" .Values.postgres.cluster.bootstrapSecretName }}
      key: password
{{- end -}}
{{- end -}}

{{- define "movienight.victoriaMetricsName" -}}
{{- printf "%s-victoriametrics" (include "movienight.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "movienight.vmagentName" -}}
{{- printf "%s-vmagent" (include "movienight.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "movienight.victoriaMetricsURL" -}}
{{- if .Values.observability.grafana.datasource.url -}}
{{- .Values.observability.grafana.datasource.url -}}
{{- else -}}
{{- .Values.observability.victoriaMetrics.url -}}
{{- end -}}
{{- end -}}

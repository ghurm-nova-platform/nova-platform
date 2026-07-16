# Nova Sandbox Worker

Isolated execution worker for builds, tests, terminal commands, repository operations, previews, and controlled agent-generated changes.

## Mandatory controls

- Ephemeral workspace per execution
- CPU, memory, disk, process, and timeout limits
- Network denied by default
- Allowlisted images and commands
- No production credentials
- Complete audit trail

This component is not permitted to execute arbitrary workloads on the Nova control-plane hosts.

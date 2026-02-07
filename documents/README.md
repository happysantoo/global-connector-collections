# Documents

Project documentation and design artifacts.

## Structure

- **design-plans/** – Detailed design and execution plans.
  - [payment-connector-framework-plan.md](design-plans/payment-connector-framework-plan.md) – High-level plan: tech stack, architecture, module layout, journalling, observability, testing, and Gradle build.
  - [connector-framework-execution-plan.md](design-plans/connector-framework-execution-plan.md) – Execution plan: milestones (M1–M9) and stories with acceptance criteria and dependencies.

- **DEMO_JMS_KAFKA_FLOW.md** – [JMS → Kafka demo flow](DEMO_JMS_KAFKA_FLOW.md): where transformation (input/output conversion) runs, where the JMS–Kafka linking bridge is wired, sequence and component diagrams (Mermaid).

- **MULTI_INPUT_OUTPUT_DESIGN.md** – [Multiple inputs and outputs](MULTI_INPUT_OUTPUT_DESIGN.md): whether the design allows multiple inputs/outputs in one process, bottleneck locations (with file/class/method), and options to support multiple outputs (composite outbound, pipeline multi-outbound, fan-out handler).

Add other document categories here as needed (e.g. architecture, guides, releases).

package com.structurizr.io.ilograph;

import com.structurizr.Workspace;
import com.structurizr.io.AbstractExporter;
import com.structurizr.io.IndentingWriter;
import com.structurizr.model.*;
import com.structurizr.util.StringUtils;
import com.structurizr.view.DynamicView;
import com.structurizr.view.ElementStyle;
import com.structurizr.view.RelationshipView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Exports a Structurizr workspace to the Ilograph definition language, for use with https://app.ilograph.com/
 */
public class IlographExporter extends AbstractExporter {

    public String export(Workspace workspace) throws Exception {
        IndentingWriter writer = new IndentingWriter();
        writer.writeLine("resources:");
        writer.writeLine();
        writer.indent();

        Model model = workspace.getModel();
        List<StaticStructureElement> elements = new ArrayList<>();

        List<Person> people = new ArrayList<>(model.getPeople());
        people.sort(Comparator.comparing(Person::getId));
        for (Person person : people) {
            writeElement(writer, workspace, person);
            elements.add(person);
        }

        List<SoftwareSystem> softwareSystems = new ArrayList<>(model.getSoftwareSystems());
        softwareSystems.sort(Comparator.comparing(SoftwareSystem::getId));
        for (SoftwareSystem softwareSystem : softwareSystems) {
            writeElement(writer, workspace, softwareSystem);
            elements.add(softwareSystem);

            if (!softwareSystem.getContainers().isEmpty()) {
                writer.indent();
                writer.writeLine("children:");
                writer.indent();

                List<Container> containers = new ArrayList<>(softwareSystem.getContainers());
                containers.sort(Comparator.comparing(Container::getId));
                for (Container container : containers) {
                    writeElement(writer, workspace, container);
                    elements.add(container);

                    if (!container.getComponents().isEmpty()) {
                        writer.indent();
                        writer.writeLine("children:");
                        writer.indent();

                        List<Component> components = new ArrayList<>(container.getComponents());
                        components.sort(Comparator.comparing(Component::getId));
                        for (Component component : components) {
                            writeElement(writer, workspace, component);
                            elements.add(component);
                        }

                        writer.outdent();
                        writer.outdent();
                    }

                }

                writer.outdent();
                writer.outdent();
            }
        }

        List<DeploymentNode> deploymentNodes = new ArrayList<>(model.getDeploymentNodes());
        deploymentNodes.sort(Comparator.comparing(DeploymentNode::getId));
        for (DeploymentNode deploymentNode : deploymentNodes) {
            writeDeploymentNode(workspace, deploymentNode, writer);
        }

        Set<Relationship> relationships = new LinkedHashSet<>();
        Set<Class> elementTypes = new HashSet<>();

        elementTypes.add(Person.class);
        elementTypes.add(SoftwareSystem.class);
        for (StaticStructureElement element : elements) {
            List<Relationship> sortedRelationships = new ArrayList<>(element.getRelationships());
            sortedRelationships.sort(Comparator.comparing(Relationship::getId));
            for (Relationship relationship : sortedRelationships) {
                if (include(relationship, elementTypes)) {
                    relationships.add(relationship);
                }
            }
        }

        elementTypes.add(Container.class);
        for (StaticStructureElement element : elements) {
            List<Relationship> sortedRelationships = new ArrayList<>(element.getRelationships());
            sortedRelationships.sort(Comparator.comparing(Relationship::getId));
            for (Relationship relationship : sortedRelationships) {
                if (include(relationship, elementTypes)) {
                    relationships.add(relationship);
                }
            }
        }

        elementTypes.add(Component.class);
        for (StaticStructureElement element : elements) {
            List<Relationship> sortedRelationships = new ArrayList<>(element.getRelationships());
            sortedRelationships.sort(Comparator.comparing(Relationship::getId));
            for (Relationship relationship : sortedRelationships) {
                if (include(relationship, elementTypes)) {
                    relationships.add(relationship);
                }
            }
        }

        writer.outdent();

        writeRelationshipsForStaticStructurePerspective(relationships, writer);

        for (DynamicView dynamicView : workspace.getViews().getDynamicViews()) {
            writeDynamicView(dynamicView, writer);
        }

        Set<String> deploymentEnvironments = new HashSet<>();
        for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
            deploymentEnvironments.add(deploymentNode.getEnvironment());
        }
        List<String> sortedDeploymentEnvironments = new ArrayList<>(deploymentEnvironments);
        sortedDeploymentEnvironments.sort(Comparator.comparing(String::toString));
        for (String deploymentEnvironment : sortedDeploymentEnvironments) {
            writeDeploymentEnvironment(workspace, deploymentEnvironment, writer);
        }

        return writer.toString();
    }

    private void writeDeploymentNode(Workspace workspace, DeploymentNode deploymentNode, IndentingWriter writer) throws Exception {
        writeElement(writer, workspace, deploymentNode);

        boolean hasChildren = !deploymentNode.getChildren().isEmpty() || !deploymentNode.getInfrastructureNodes().isEmpty() || !deploymentNode.getSoftwareSystemInstances().isEmpty() || !deploymentNode.getContainerInstances().isEmpty();

        if (hasChildren) {
            writer.indent();
            writer.writeLine("children:");
            writer.indent();
        }

        List<DeploymentNode> deploymentNodes = new ArrayList<>(deploymentNode.getChildren());
        deploymentNodes.sort(Comparator.comparing(DeploymentNode::getId));
        for (DeploymentNode child : deploymentNodes) {
            writeDeploymentNode(workspace, child, writer);
        }

        List<InfrastructureNode> infrastructureNodes = new ArrayList<>(deploymentNode.getInfrastructureNodes());
        infrastructureNodes.sort(Comparator.comparing(InfrastructureNode::getId));
        for (InfrastructureNode infrastructureNode : infrastructureNodes) {
            writeElement(writer, workspace, infrastructureNode);
        }

        List<SoftwareSystemInstance> softwareSystemInstances = new ArrayList<>(deploymentNode.getSoftwareSystemInstances());
        softwareSystemInstances.sort(Comparator.comparing(SoftwareSystemInstance::getId));
        for (SoftwareSystemInstance softwareSystemInstance : softwareSystemInstances) {
            writeElement(writer, workspace, softwareSystemInstance);
        }

        List<ContainerInstance> containerInstances = new ArrayList<>(deploymentNode.getContainerInstances());
        containerInstances.sort(Comparator.comparing(ContainerInstance::getId));
        for (ContainerInstance containerInstance : containerInstances) {
            writeElement(writer, workspace, containerInstance);
        }

        writer.outdent();
        writer.outdent();
    }

    private void writeElement(IndentingWriter writer, Workspace workspace, Element element) throws Exception {
        writer.writeLine(String.format("- id: \"%s\"", element.getId()));

        String name;
        String type;
        String description;
        ElementStyle elementStyle;

        if (element instanceof StaticStructureElementInstance) {
            StaticStructureElementInstance elementInstance = (StaticStructureElementInstance)element;
            name = elementInstance.getElement().getName();
            type = typeOf(workspace, elementInstance.getElement(), true);
            description = elementInstance.getElement().getDescription();
            elementStyle = workspace.getViews().getConfiguration().getStyles().findElementStyle(elementInstance.getElement());
        } else {
            name = element.getName();
            type = typeOf(workspace, element, true);
            description = element.getDescription();
            elementStyle = workspace.getViews().getConfiguration().getStyles().findElementStyle(element);
        }

        writer.indent();
        writer.writeLine(String.format("name: \"%s\"", name));
        writer.writeLine(String.format("subtitle: \"%s\"", type));

        if (!StringUtils.isNullOrEmpty(description)) {
            writer.writeLine(String.format("description: \"%s\"", description));
        }

        if (element instanceof DeploymentNode) {
            writer.writeLine(String.format("backgroundColor: \"%s\"", "#ffffff"));
        } else {
            writer.writeLine(String.format("backgroundColor: \"%s\"", elementStyle.getBackground()));
        }
        writer.writeLine(String.format("color: \"%s\"", elementStyle.getColor()));
        writer.writeLine();
        writer.outdent();
    }

    private void writeRelationshipsForStaticStructurePerspective(Collection<Relationship> relationships, IndentingWriter writer) throws Exception {
        writer.writeLine("perspectives:");
        writer.indent();
        writer.writeLine("- name: Static Structure");
        writer.indent();
        writer.writeLine("relations:");
        writer.indent();

        for (Relationship relationship : relationships) {
            writer.writeLine(String.format("- from: \"%s\"", relationship.getSourceId()));
            writer.indent();
            writer.writeLine(String.format("to: \"%s\"", relationship.getDestinationId()));

            if (!StringUtils.isNullOrEmpty(relationship.getDescription())) {
                writer.writeLine(String.format("label: \"%s\"", relationship.getDescription()));
            }

            if (!StringUtils.isNullOrEmpty(relationship.getTechnology())) {
                writer.writeLine(String.format("description: \"%s\"", relationship.getTechnology()));
            }

            writer.writeLine();
            writer.outdent();
        }

        writer.outdent();
        writer.outdent();
        writer.outdent();
    }

    private void writeDynamicView(DynamicView dynamicView, IndentingWriter writer) throws Exception {
        writer.indent();
        writer.writeLine("- name: Dynamic - " + dynamicView.getName());
        writer.indent();
        writer.writeLine("sequence:");

        int count = 0;
        for (RelationshipView relationshipView : dynamicView.getRelationships()) {
            Relationship relationship = relationshipView.getRelationship();
            if (count == 0) {
                writer.indent();
                writer.writeLine(String.format("start: \"%s\"", relationship.getSourceId()));
                writer.writeLine("steps:");
                writer.writeLine(String.format("- to: \"%s\"", relationship.getDestinationId()));
            } else {
                if (relationshipView.isResponse() != null && relationshipView.isResponse()) {
                    writer.writeLine(String.format("- to: \"%s\"", relationship.getSourceId()));
                } else {
                    writer.writeLine(String.format("- to: \"%s\"", relationship.getDestinationId()));
                }
            }

            writer.indent();
            if (!StringUtils.isNullOrEmpty(relationshipView.getDescription())) {
                writer.writeLine(String.format("label: \"%s. %s\"", relationshipView.getOrder(), relationshipView.getDescription()));
            } else if (!StringUtils.isNullOrEmpty(relationship.getDescription())) {
                writer.writeLine(String.format("label: \"%s. %s\"", relationshipView.getOrder(), relationship.getDescription()));
            }

            if (!StringUtils.isNullOrEmpty(relationship.getTechnology())) {
                writer.writeLine(String.format("description: \"%s\"", relationship.getTechnology()));
            }
            writer.outdent();

            writer.writeLine();

            count++;
        }

        writer.outdent();
        writer.outdent();
        writer.outdent();
    }

    private void writeDeploymentEnvironment(Workspace workspace, String deploymentEnvironment, IndentingWriter writer) throws Exception {
        writer.indent();
        writer.writeLine("- name: Deployment - " + deploymentEnvironment);
        writer.indent();
        writer.writeLine("relations:");

        List<DeploymentNode> topLevelDeploymentNodes = workspace.getModel().getDeploymentNodes().stream().filter(dn -> dn.getEnvironment().equals(deploymentEnvironment)).sorted(Comparator.comparing(DeploymentNode::getId)).collect(Collectors.toList());
        List<Element> deploymentElementsInEnvironment = new ArrayList<>(topLevelDeploymentNodes);
        for (DeploymentNode deploymentNode : topLevelDeploymentNodes) {
            deploymentElementsInEnvironment.addAll(findAllChildren(deploymentNode));
        }

        Collection<Relationship> relationships = findRelationships(deploymentElementsInEnvironment);
        writer.indent();

        for (Relationship relationship : relationships) {
            writer.writeLine(String.format("- from: \"%s\"", relationship.getSourceId()));
            writer.indent();
            writer.writeLine(String.format("to: \"%s\"", relationship.getDestinationId()));

            if (!StringUtils.isNullOrEmpty(relationship.getDescription())) {
                writer.writeLine(String.format("label: \"%s\"", relationship.getDescription()));
            }

            if (!StringUtils.isNullOrEmpty(relationship.getTechnology())) {
                writer.writeLine(String.format("description: \"%s\"", relationship.getTechnology()));
            }

            writer.outdent();
        }

        writer.outdent();
        writer.outdent();
        writer.outdent();
    }

    private Collection<Element> findAllChildren(DeploymentNode deploymentNode) {
        List<Element> deploymentElements = new ArrayList<>();

        List<DeploymentNode> deploymentNodes = new ArrayList<>(deploymentNode.getChildren());
        deploymentNodes.sort(Comparator.comparing(DeploymentNode::getId));
        for (DeploymentNode child : deploymentNodes) {
            deploymentElements.addAll(findAllChildren(child));
        }

        deploymentElements.addAll(deploymentNode.getSoftwareSystemInstances().stream().sorted(Comparator.comparing(SoftwareSystemInstance::getId)).collect(Collectors.toList()));
        deploymentElements.addAll(deploymentNode.getContainerInstances().stream().sorted(Comparator.comparing(ContainerInstance::getId)).collect(Collectors.toList()));
        deploymentElements.addAll(deploymentNode.getInfrastructureNodes().stream().sorted(Comparator.comparing(InfrastructureNode::getId)).collect(Collectors.toList()));

        return deploymentElements;
    }

    private Collection<Relationship> findRelationships(Collection<Element> elements) {
        List<Relationship> relationships = new ArrayList<>();

        for (Element element : elements) {
            List<Relationship> sortedRelationships = new ArrayList<>(element.getRelationships());
            sortedRelationships.sort(Comparator.comparing(Relationship::getId));
            for (Relationship relationship : sortedRelationships) {
                if (elements.contains(relationship.getSource()) && elements.contains(relationship.getDestination())) {
                    relationships.add(relationship);
                }
            }
        }

        return relationships;
    }

    private boolean include(Relationship relationship, Set<Class> elementTypes) {
        Element source = relationship.getSource();
        Element destination = relationship.getDestination();

        return elementTypes.contains(source.getClass()) && elementTypes.contains(destination.getClass());
    }

}
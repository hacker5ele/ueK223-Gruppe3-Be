package com.example.demo.domain.group;

import com.example.demo.core.generic.AbstractServiceImpl;
import com.example.demo.domain.group.dto.GroupCreateDTO;
import com.example.demo.domain.user.User;
import com.example.demo.domain.user.UserService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl extends AbstractServiceImpl<Group> implements GroupService {

    private final GroupRepository groupRepository;
    private final UserService userService;

    private static final String GROUP_NOT_FOUND = "Group not found with id: ";

    public GroupServiceImpl(GroupRepository groupRepository, UserService userService) {
        super(groupRepository);
        this.groupRepository = groupRepository;
        this.userService = userService;
    }

    @Override
    public List<Group> findAllGroups() {
        return groupRepository.findAll();
    }

    @Override
    public Group findGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(GROUP_NOT_FOUND + groupId));
    }

    @Override
    public Group createGroup(GroupCreateDTO dto) {
        if (groupRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Group name already exists: " + dto.getName());
        }

        User admin = userService.findById(dto.getAdministratorId());
        Set<User> members = new HashSet<>();
        if (dto.getMemberIds() != null) {
            members = dto.getMemberIds()
                    .stream()
                    .map(userService::findById)
                    .collect(Collectors.toSet());
        }

        Group group = new Group()
                .setName(dto.getName())
                .setMotto(dto.getMotto())
                .setLogo(dto.getLogo())
                .setAdministrator(admin)
                .setMembers(members);

        return groupRepository.save(group);
    }

    @Override
    public Group updateGroup(UUID groupId, GroupCreateDTO dto) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(GROUP_NOT_FOUND + groupId));
        if (!group.getName().equals(dto.getName()) && groupRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Group name already exists: " + dto.getName());
        }

        group.setName(dto.getName());
        group.setMotto(dto.getMotto());
        group.setLogo(dto.getLogo());

        if (dto.getMemberIds() != null) {
            group.setMembers(dto.getMemberIds()
                    .stream()
                    .map(userService::findById)
                    .collect(Collectors.toSet()));
        }

        return groupRepository.save(group);
    }

    @Override
    public void deleteGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(GROUP_NOT_FOUND + groupId));
        group.getMembers().forEach(member -> member.setGroup(null)); // detach members
        groupRepository.delete(group);
    }

    @Override
    public void joinGroup(UUID userId, UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(GROUP_NOT_FOUND + groupId));
        User user = userService.findById(userId);

        if (user.getGroup() != null) {
            throw new IllegalArgumentException("User is already part of a group: " + user.getGroup().getName());
        }
        user.setGroup(group);
        group.getMembers().add(user);
        groupRepository.save(group);
    }
}

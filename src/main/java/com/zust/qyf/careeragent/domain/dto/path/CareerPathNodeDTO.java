package com.zust.qyf.careeragent.domain.dto.path;

import java.util.List;

public record CareerPathNodeDTO(
        String current,
        int level,
        List<CareerPathNodeDTO> paths
) {
}

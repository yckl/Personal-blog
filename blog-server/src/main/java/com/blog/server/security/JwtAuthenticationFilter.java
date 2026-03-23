package com.blog.server.security;

import com.blog.server.entity.Member;
import com.blog.server.entity.SysUser;
import com.blog.server.mapper.MemberMapper;
import com.blog.server.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;
    private final MemberMapper memberMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            String tokenType = jwtTokenProvider.getTokenType(token);

            if ("access".equals(tokenType)) {
                // Admin user token
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                SysUser user = sysUserMapper.selectById(userId);

                if (user != null && user.getStatus() == 1) {
                    List<String> roles = sysUserMapper.selectRoleKeysByUserId(userId);
                    List<String> permissions = sysUserMapper.selectPermissionCodesByUserId(userId);

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                    permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else if ("member".equals(tokenType)) {
                // Member token
                Long memberId = jwtTokenProvider.getUserIdFromToken(token);
                Member member = memberMapper.selectById(memberId);

                if (member != null && "ACTIVE".equals(member.getStatus())) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
                    // Premium members get an additional role
                    if ("PREMIUM".equals(member.getTier())) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_PREMIUM"));
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(member, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
